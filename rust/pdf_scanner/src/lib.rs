//! Native PDF file scanner for Android
//! 
//! Uses walkdir for fast directory traversal, significantly faster than Java/Kotlin file walking.

use jni::JNIEnv;
use jni::objects::{JClass, JObject, JString, JValue};
use jni::sys::jobjectArray;

/// Initialize Android logging
#[cfg(target_os = "android")]
fn init_logging() {
    android_logger::init_once(
        android_logger::Config::default()
            .with_max_level(log::LevelFilter::Debug)
            .with_tag("NativeScanner"),
    );
}

#[cfg(not(target_os = "android"))]
fn init_logging() {}

/// Scan directory for PDF files
/// Returns array of file paths
#[no_mangle]
pub extern "C" fn Java_com_hyntix_android_pdfmanager_native_NativeScanner_scanPdfs(
    mut env: JNIEnv,
    _class: JClass,
    root_path: JString,
) -> jobjectArray {
    init_logging();
    
    // Convert Java string to Rust string
    let root: String = match env.get_string(&root_path) {
        Ok(s) => s.into(),
        Err(_) => {
            log::error!("Failed to get root path string");
            return std::ptr::null_mut();
        }
    };
    
    log::debug!("Scanning directory: {}", root);
    
    // Collect PDF paths
    let mut pdf_paths: Vec<String> = Vec::new();
    
    for entry in walkdir::WalkDir::new(&root)
        .follow_links(false)
        .into_iter()
        .filter_entry(|e| {
            // Skip hidden directories and common non-PDF directories
            let name = e.file_name().to_str().unwrap_or("");
            !name.starts_with('.') && 
            name != "Android" &&  // Skip Android system directories
            name != "data"
        })
        .filter_map(|e| e.ok())
    {
        let path = entry.path();
        
        if path.is_file() {
            if let Some(ext) = path.extension() {
                if ext.to_str().map(|s| s.eq_ignore_ascii_case("pdf")).unwrap_or(false) {
                    if let Some(path_str) = path.to_str() {
                        pdf_paths.push(path_str.to_string());
                    }
                }
            }
        }
    }
    
    log::debug!("Found {} PDF files", pdf_paths.len());
    
    // Create Java String array
    let string_class = match env.find_class("java/lang/String") {
        Ok(c) => c,
        Err(_) => return std::ptr::null_mut(),
    };
    
    let array = match env.new_object_array(
        pdf_paths.len() as i32,
        &string_class,
        JObject::null(),
    ) {
        Ok(a) => a,
        Err(_) => return std::ptr::null_mut(),
    };
    
    for (i, path) in pdf_paths.iter().enumerate() {
        if let Ok(jstr) = env.new_string(path) {
            let _ = env.set_object_array_element(&array, i as i32, jstr);
        }
    }
    
    array.into_raw()
}

/// Scan directory and return file info (path, size, modified time)
/// Returns array of FileInfo objects
#[no_mangle]
pub extern "C" fn Java_com_hyntix_android_pdfmanager_native_NativeScanner_scanPdfsWithInfo(
    mut env: JNIEnv,
    _class: JClass,
    root_path: JString,
) -> jobjectArray {
    init_logging();
    
    let root: String = match env.get_string(&root_path) {
        Ok(s) => s.into(),
        Err(_) => return std::ptr::null_mut(),
    };
    
    // Find FileInfo class
    let file_info_class = match env.find_class("com/hyntix/android/pdfmanager/native/NativeScanner$FileInfo") {
        Ok(c) => c,
        Err(e) => {
            log::error!("Failed to find FileInfo class: {:?}", e);
            return std::ptr::null_mut();
        }
    };
    
    // Collect PDF info
    let mut pdf_files: Vec<(String, i64, i64)> = Vec::new();  // (path, size, modified)
    
    for entry in walkdir::WalkDir::new(&root)
        .follow_links(false)
        .into_iter()
        .filter_entry(|e| {
            let name = e.file_name().to_str().unwrap_or("");
            !name.starts_with('.') && name != "Android" && name != "data"
        })
        .filter_map(|e| e.ok())
    {
        let path = entry.path();
        
        if path.is_file() {
            if let Some(ext) = path.extension() {
                if ext.to_str().map(|s| s.eq_ignore_ascii_case("pdf")).unwrap_or(false) {
                    if let (Some(path_str), Ok(metadata)) = (path.to_str(), path.metadata()) {
                        let size = metadata.len() as i64;
                        let modified = metadata.modified()
                            .map(|t| t.duration_since(std::time::UNIX_EPOCH)
                                .map(|d| d.as_millis() as i64)
                                .unwrap_or(0))
                            .unwrap_or(0);
                        
                        pdf_files.push((path_str.to_string(), size, modified));
                    }
                }
            }
        }
    }
    
    log::debug!("Found {} PDF files with info", pdf_files.len());
    
    // Create array of FileInfo objects
    let array = match env.new_object_array(
        pdf_files.len() as i32,
        &file_info_class,
        JObject::null(),
    ) {
        Ok(a) => a,
        Err(_) => return std::ptr::null_mut(),
    };
    
    for (i, (path, size, modified)) in pdf_files.iter().enumerate() {
        if let Ok(jpath) = env.new_string(path) {
            // Call FileInfo constructor: FileInfo(path: String, size: Long, lastModified: Long)
            if let Ok(file_info) = env.new_object(
                &file_info_class,
                "(Ljava/lang/String;JJ)V",
                &[
                    JValue::Object(&jpath),
                    JValue::Long(*size),
                    JValue::Long(*modified),
                ],
            ) {
                let _ = env.set_object_array_element(&array, i as i32, file_info);
            }
        }
    }
    
    array.into_raw()
}

/// Find duplicate PDF files using content hashing
/// Returns array of DuplicateGroup objects (hash, file paths)
#[no_mangle]
pub extern "C" fn Java_com_hyntix_android_pdfmanager_native_NativeScanner_findDuplicates(
    mut env: JNIEnv,
    _class: JClass,
    root_path: JString,
) -> jobjectArray {
    init_logging();
    
    let root: String = match env.get_string(&root_path) {
        Ok(s) => s.into(),
        Err(_) => return std::ptr::null_mut(),
    };
    
    log::debug!("Finding duplicates in: {}", root);
    
    // Phase 1: Collect all PDF files with their sizes
    let mut files_by_size: std::collections::HashMap<u64, Vec<String>> = std::collections::HashMap::new();
    
    for entry in walkdir::WalkDir::new(&root)
        .follow_links(false)
        .into_iter()
        .filter_entry(|e| {
            let name = e.file_name().to_str().unwrap_or("");
            !name.starts_with('.') && name != "Android" && name != "data"
        })
        .filter_map(|e| e.ok())
    {
        let path = entry.path();
        
        if path.is_file() {
            if let Some(ext) = path.extension() {
                if ext.to_str().map(|s| s.eq_ignore_ascii_case("pdf")).unwrap_or(false) {
                    if let (Some(path_str), Ok(metadata)) = (path.to_str(), path.metadata()) {
                        let size = metadata.len();
                        files_by_size.entry(size).or_default().push(path_str.to_string());
                    }
                }
            }
        }
    }
    
    log::debug!("Phase 1: Found {} unique sizes", files_by_size.len());
    
    // Phase 2 & 3: For groups with multiple files, compute hashes
    let mut duplicates_by_hash: std::collections::HashMap<String, Vec<String>> = std::collections::HashMap::new();
    
    for (_size, paths) in files_by_size.into_iter().filter(|(_, v)| v.len() > 1) {
        // Phase 2: Partial hash (first 4KB + last 4KB)
        let mut partial_hash_groups: std::collections::HashMap<String, Vec<String>> = std::collections::HashMap::new();
        
        for path in &paths {
            if let Ok(partial_hash) = compute_partial_hash(path) {
                partial_hash_groups.entry(partial_hash).or_default().push(path.clone());
            }
        }
        
        // Phase 3: Full hash for partial hash matches
        for (_, matching_paths) in partial_hash_groups.into_iter().filter(|(_, v)| v.len() > 1) {
            for path in matching_paths {
                if let Ok(full_hash) = compute_full_hash(&path) {
                    duplicates_by_hash.entry(full_hash).or_default().push(path);
                }
            }
        }
    }
    
    // Filter to only groups with actual duplicates (2+ files with same hash)
    let duplicate_groups: Vec<(String, Vec<String>)> = duplicates_by_hash
        .into_iter()
        .filter(|(_, v)| v.len() > 1)
        .collect();
    
    log::debug!("Found {} duplicate groups", duplicate_groups.len());
    
    // Create Java array of DuplicateGroup objects
    let group_class = match env.find_class("com/hyntix/android/pdfmanager/native/NativeScanner$DuplicateGroup") {
        Ok(c) => c,
        Err(e) => {
            log::error!("Failed to find DuplicateGroup class: {:?}", e);
            return std::ptr::null_mut();
        }
    };
    
    let array = match env.new_object_array(duplicate_groups.len() as i32, &group_class, JObject::null()) {
        Ok(a) => a,
        Err(_) => return std::ptr::null_mut(),
    };
    
    let string_class = match env.find_class("java/lang/String") {
        Ok(c) => c,
        Err(_) => return std::ptr::null_mut(),
    };
    
    for (i, (hash, paths)) in duplicate_groups.iter().enumerate() {
        // Create String array for paths
        let paths_array = match env.new_object_array(paths.len() as i32, &string_class, JObject::null()) {
            Ok(a) => a,
            Err(_) => continue,
        };
        
        for (j, path) in paths.iter().enumerate() {
            if let Ok(jpath) = env.new_string(path) {
                let _ = env.set_object_array_element(&paths_array, j as i32, jpath);
            }
        }
        
        // Create DuplicateGroup object
        if let Ok(jhash) = env.new_string(hash) {
            if let Ok(group) = env.new_object(
                &group_class,
                "(Ljava/lang/String;[Ljava/lang/String;)V",
                &[
                    JValue::Object(&jhash),
                    JValue::Object(&paths_array),
                ],
            ) {
                let _ = env.set_object_array_element(&array, i as i32, group);
            }
        }
    }
    
    array.into_raw()
}

/// Compute partial hash (first 4KB + last 4KB) for quick filtering
fn compute_partial_hash(path: &str) -> Result<String, std::io::Error> {
    use std::io::{Read, Seek, SeekFrom};
    
    let mut file = std::fs::File::open(path)?;
    let metadata = file.metadata()?;
    let size = metadata.len();
    
    let mut buffer = Vec::new();
    
    // Read first 4KB
    let first_chunk_size = std::cmp::min(4096, size) as usize;
    let mut first_chunk = vec![0u8; first_chunk_size];
    file.read_exact(&mut first_chunk)?;
    buffer.extend_from_slice(&first_chunk);
    
    // Read last 4KB (if file is large enough)
    if size > 8192 {
        file.seek(SeekFrom::End(-4096))?;
        let mut last_chunk = vec![0u8; 4096];
        file.read_exact(&mut last_chunk)?;
        buffer.extend_from_slice(&last_chunk);
    }
    
    let digest = md5::compute(&buffer);
    Ok(format!("{:x}", digest))
}

/// Compute full MD5 hash of file content
fn compute_full_hash(path: &str) -> Result<String, std::io::Error> {
    let content = std::fs::read(path)?;
    let digest = md5::compute(&content);
    Ok(format!("{:x}", digest))
}
