package com.hyntix.android.pdfmanager.ui.settings

object PolicyContent {
    
    const val PRIVACY_POLICY = """
**Last updated: January 2026**

This Privacy Policy describes Our policies and procedures on the collection, use and disclosure of Your information when You use the Service and tells You about Your privacy rights and how the law protects You.

## 1. Information Collection and Use

**Personal Data**: We do not collect, store, or transmit any personally identifiable information (PII) such as your name, email address, or phone number. You do not need to create an account to use the Application.

**File Access and Security**: The Application operates locally on Your device. We do not upload Your PDF files or documents to any external server or cloud service. All rendering, editing, and management of files occur strictly within Your device's local environment.

## 2. Permissions and Device Access

To provide core functionality, the Application requests specific permissions:
*   **Storage Access (Manage External Storage)**: This permission is essential for the Application's core purpose as a file manager and viewer. It requires `MANAGE_EXTERNAL_STORAGE` on Android 11+ to allow You to browse, organize, rename, delete, and view PDF files across your device storage. We only access files that You explicitly interact with or that are relevant to the app's functionality (PDFs).

## 3. Children's Privacy

Our Service does not address anyone under the age of 13. We do not knowingly collect personally identifiable information from anyone under the age of 13.

## 4. Changes to this Privacy Policy
We may update Our Privacy Policy from time to time. We will notify You of any changes by posting the new Privacy Policy on this page.

## Contact Us
If you have any questions about this Privacy Policy, You can contact us at: support@hyntix.com
"""

    const val TERMS_OF_SERVICE = """
**Last updated: January 2026**

## 1. Agreement to Terms
By downloading, accessing, or using the PDF Manager application, you agree to be bound by these Terms of Service. If you do not agree to these terms, please do not use the Application.

## 2. Usage License
Subject to your compliance with these Terms, We grant you a limited, non-exclusive, non-transferable, revocable license to download and use the Application for your personal, non-commercial use on a device owned or controlled by you.

## 3. User Responsibilities
*   **Content**: You are solely responsible for the content of the files you view, manage, or share using the Application. We do not monitor or control the content of your files.
*   **Lawful Use**: You agree not to use the Application for any unlawful purpose or in any way that violates the rights of others.

## 4. Intellectual Property
The Application, including its code, design, and original content, is the property of Hyntix and is protected by copyright and other intellectual property laws.

## 5. Disclaimer of Warranties
The Application is provided "AS IS" and "AS AVAILABLE" without warranties of any kind, whether express or implied, including, but not limited to, implied warranties of merchantability, fitness for a particular purpose, or non-infringement. We do not warrant that the Application will be error-free or uninterrupted.

## 6. Limitation of Liability
To the maximum extent permitted by law, We shall not be liable for any indirect, incidental, special, consequential, or punitive damages, or any loss of profits or data, arising out of or in connection with your use of the Application.

## 7. Governing Law
These Terms shall be governed by and construed in accordance with the laws of the jurisdiction in which the developer resides, without regard to its conflict of law provisions.

## 8. Changes to Terms
We reserve the right to modify these Terms at any time. Your continued use of the Application after any such changes constitutes your acceptance of the new Terms.
"""


    const val OPEN_SOURCE_LICENSES = """
## Open Source Libraries

This application uses the following open-source libraries:

### Android Jetpack
*   **AndroidX Libraries**: Licensed under the Apache License, Version 2.0.
*   **Jetpack Compose**: Licensed under the Apache License, Version 2.0.
*   **Navigation Component**: Licensed under the Apache License, Version 2.0.

### Native Components
*   **Pdfium (via KotlinPdfium)**: Licensed under the GNU AGPL v3.0.
*   **HyntixPdfViewer**: Licensed under the GNU AGPL v3.0.

### Icons & UI
*   **Phosphor Icons**: Licensed under the MIT License.

---

### GNU Affero General Public License, Version 3.0
The core PDF rendering and viewing components of this application are licensed under the GNU Affero General Public License v3.0 (AGPL-3.0). You can find the full license text in the application repository.

### Apache License, Version 2.0
Licensed under the Apache License, Version 2.0 (the "License"); you may not use these files except in compliance with the License. You may obtain a copy of the License at:
http://www.apache.org/licenses/LICENSE-2.0

### MIT License
Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
"""
}
