# Sprint Reflection: Resolving the Maven Build and Deployment Issue

## Overview

During this sprint, our primary technical challenge was fixing a persistent Maven build and deployment failure in our CI/CD pipeline, specifically related to publishing artifacts to GitHub Packages. This reflection captures the process, learnings, and outcomes from addressing this issue.

---

## Problem Statement

The build failed during the `mvn deploy` step, with an error message indicating:
```
Could not find artifact com.santhan:banking-system:pom:0.0.1-20250605.080200-1 in github (https://maven.pkg.github.com/SANTHAN-KUMAR/Banking-system)
```
This suggested Maven could not authenticate or push artifacts to GitHub Packages, blocking our CI/CD process.

---

## Root Cause Analysis

- The local Maven configuration (`~/.m2/settings.xml`) was missing or incorrectly configured for authentication.
- The required GitHub Personal Access Token (PAT) was not set, or the XML tags were malformed.
- The CI pipeline (GitHub Actions) also required proper credentials to deploy.

---

## Steps Taken

1. **Identified Authentication Gap**  
   We traced the failure to a missing/incorrect `<server>` configuration for the GitHub Packages repository in `settings.xml`.

2. **Generated a GitHub Personal Access Token**  
   - Created a token with `repo`, `read:packages`, and `write:packages` scopes.

3. **Updated Maven Configuration**  
   - Added the following to `~/.m2/settings.xml`:
     ```xml
     <settings>
       <servers>
         <server>
           <id>github</id>
           <username>SANTHAN-KUMAR</username>
           <password>YOUR_PERSONAL_ACCESS_TOKEN</password>
         </server>
       </servers>
     </settings>
     ```
   - Fixed a typo where `<passKord>` was mistakenly used instead of `<password>`.

4. **Verified and Updated `pom.xml`**  
   - Ensured the `<distributionManagement>` section matched the repository ID:
     ```xml
     <distributionManagement>
       <repository>
         <id>github</id>
         <url>https://maven.pkg.github.com/SANTHAN-KUMAR/Banking-system</url>
       </repository>
     </distributionManagement>
     ```

5. **Tested Local Deployment**  
   - Successfully ran `mvn clean deploy`, confirming the artifact appeared in GitHub Packages.

6. **Configured CI/CD (GitHub Actions)**  
   - Updated the workflow to set up `settings.xml` with `${{ secrets.GITHUB_TOKEN }}` for authentication in CI.
   - Example step:
     ```yaml
     - name: Configure Maven for GitHub Packages
       run: |
         mkdir -p ~/.m2
         echo "<settings>...</settings>" > ~/.m2/settings.xml
     ```

7. **Validated the Fix**  
   - Reran the workflow. The deployment step completed successfully.
   - The package appeared under the GitHub Packages section for the repository.

---

## Lessons Learned

- **Attention to Detail in Configuration:**  
  Small typos (e.g., `<passKord>` instead of `<password>`) can lead to major build failures.
- **The Importance of Secure Credentials:**  
  PATs must be handled securely and not committed to source control.
- **Replicating CI Issues Locally:**  
  Testing deployment locally can speed up debugging before relying on CI runs.
- **Documentation and Automation:**  
  Keeping clear documentation and automating CI/CD setup prevents future regressions.

---

## Next Steps

- Automate Maven settings configuration in all CI environments.
- Document the deployment process and troubleshooting steps in the project README.
- Regularly review and rotate access tokens for security.

---

**This experience reinforced the importance of careful configuration, secure credential management, and robust automation in maintaining a healthy CI/CD pipeline.**
