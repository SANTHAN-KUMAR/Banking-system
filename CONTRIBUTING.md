# Contributing to Banking System

We welcome contributions to the Banking System project! By participating in this project, you agree to abide by our Code of Conduct.

## Table of Contents

* [Code of Conduct](#code-of-conduct)
* [How Can I Contribute?](#how-can-i-contribute)
    * [Reporting Bugs](#reporting-bugs)
    * [Suggesting Enhancements](#suggesting-enhancements)
    * [Your First Code Contribution](#your-first-code-contribution)
    * [Pull Request Guidelines](#pull-request-guidelines)
* [Setting Up Your Development Environment](#setting-up-your-development-environment)
* [Coding Style](#coding-style)
* [License](#license)

## Code of Conduct

This project and everyone participating in it is governed by the [Code of Conduct](CODE_OF_CONDUCT.md). By participating, you are expected to uphold this code. Please report unacceptable behavior to [your-email@example.com](mailto:your-email@example.com).

## How Can I Contribute?

There are several ways you can contribute to the Banking System project.

### Reporting Bugs

If you find a bug, please help us by [submitting an issue on GitHub](https://github.com/santhan/banking-system/issues). Before you do, please search existing issues to see if the bug has already been reported.

When reporting a bug, please include as many details as possible:
* A clear and concise description of the bug.
* Steps to reproduce the behavior.
* Expected behavior.
* Screenshots (if applicable).
* Your environment (OS, Java version, Spring Boot version, etc.).

### Suggesting Enhancements

Do you have an idea for a new feature or an improvement to an existing one? We'd love to hear it!
* [Open an issue on GitHub](https://github.com/santhan/banking-system/issues) to propose your idea.
* Clearly describe the enhancement and why it would be beneficial to the project.
* Provide examples or use cases if possible.

### Your First Code Contribution

If you're looking for an easy way to get started, look for issues labeled "good first issue" or "help wanted" in our [issue tracker](https://github.com/santhan/banking-system/issues).

### Pull Request Guidelines

Follow these steps to make a significant contribution:

1.  **Fork the repository:** Start by forking the `santhan/banking-system` repository to your GitHub account.
2.  **Clone your fork:**
    ```bash
    git clone [https://github.com/YOUR_USERNAME/banking-system.git](https://github.com/YOUR_USERNAME/banking-system.git)
    cd banking-system
    ```
3.  **Create a new branch:**
    ```bash
    git checkout -b feature/your-feature-name
    ```
    (or `bugfix/your-bug-name` for bug fixes)
4.  **Make your changes:** Implement your feature or bug fix.
5.  **Write tests:** Ensure your changes are well-tested. New features should have unit and/or integration tests. Bug fixes should include a test that reproduces the bug.
6.  **Run tests locally:** Before pushing, ensure all existing tests (and your new ones) pass.
    ```bash
    cd "Project files" # Assuming your pom.xml is in Project files/
    mvn clean install
    ```
7.  **Commit your changes:** Write clear, concise commit messages.
    ```bash
    git commit -m "feat: Add new feature (brief description)"
    # or "fix: Resolve bug (brief description)"
    ```
8.  **Push your branch to your fork:**
    ```bash
    git push origin feature/your-feature-name
    ```
9.  **Open a Pull Request:** Go to the original `santhan/banking-system` repository on GitHub and open a new Pull Request from your branch.
    * Provide a clear title and description for your PR.
    * Reference any related issues (e.g., `Closes #123`).
    * Ensure your PR addresses only one logical change. If you have multiple unrelated changes, create separate PRs.

## Setting Up Your Development Environment

To get started with local development:

1.  **Clone the repository:**
    ```bash
    git clone [https://github.com/santhan/banking-system.git](https://github.com/santhan/banking-system.git)
    cd banking-system
    ```
2.  **Navigate to the project root:**
    ```bash
    cd "Project files"
    ```
3.  **Install Java Development Kit (JDK) 23.**
4.  **Install Apache Maven.**
5.  **Set up your MySQL Database:**
    * Ensure MySQL server is running locally on `localhost:3306`.
    * Create a database named `banking_system`.
    * Your `application.properties` will connect to this database.
6.  **Build the project:**
    ```bash
    mvn clean install
    ```
7.  **Run the application:**
    ```bash
    mvn spring-boot:run
    ```

## Coding Style

* Follow standard Java conventions.
* Adhere to Spring Boot best practices.
* Keep your code clean, readable, and well-commented where necessary.
* Use Lombok annotations as appropriate (already configured).

## License

By contributing, you agree that your contributions will be licensed under the [MIT License](LICENSE.md) (or whatever license your project uses).
