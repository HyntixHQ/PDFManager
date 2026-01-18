# Security Policy

## Reporting a Vulnerability

If you discover a security vulnerability, please report it responsibly.

**Do not** create a public GitHub issue for security vulnerabilities.

### Contact

Email: security@hyntix.com

### What to Include

1. Description of the vulnerability
2. Steps to reproduce
3. Potential impact
4. Suggested fix (if any)

### Response Timeline

- **Acknowledgment**: Within 48 hours
- **Initial Assessment**: Within 7 days
- **Resolution**: Critical issues within 30 days

## Scope

In scope:
- PDF Manager application code
- Dependencies maintained by this project

Out of scope:
- Third-party dependencies (report to their maintainers)
- Social engineering attacks

## Data Handling

PDF Manager processes all data locally:

| Data | Storage | Purpose |
|------|---------|---------|
| Recent Files | Room DB | Quick access |
| Favorites | Room DB | Bookmarks |
| Virtual Folders | Room DB | Organization |
| Settings | DataStore | Preferences |
| Cache | App Cache | Performance |

**No data is transmitted externally.**
