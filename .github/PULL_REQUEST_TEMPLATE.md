### Description
Please describe your pull request important details.

### Author checklist for this pull request
🚨 Code Review Checklist (Mandatory) :

- [ ]  Ensure Git branching and commit message guidelines followed.
- [ ]  Ensure no conflicts in the pull request and pull is taken from origin branch.
- [ ]  Ensure good naming convention is followed for variables, constants, functions, classes, and package as defined in
  
    | # | Guideline (GDL)                       |
    |:-:|:--------------------------------------|
    | 1 | GDL-005 Coding Standards – Java       |
    | 2 | GDL-006 Coding Standards – JavaScript |
    | 3 | GDL-007 Coding Standards - PLSQL      |

Use domain-based naming conventions like caseObject, reportObject, signalObject.
- [ ]  Ensure proper logging information (info, error, or debug).
- [ ]  Ensure code styling, code alignment, proper spacing, code comments, and error handling. Ensure the best and proper data type used instead of using 'def' (in groovy) or any generic data type.
- [ ]  Ensure code additions pass linting checks and unit tests, ensuring sufficient coverage to prevent disruptions to existing functionality.
- [ ]  Ensure unit test added/updated.
- [ ]  Ensure IDE (in-built) feature of code inspection is performed, and changes will not introduce redundant code, and code formatting issues.
- [ ]  Ensure code performance when loops are involved, and provide evidence when required.
- [ ]  Ensure new configurable parameter(s) is/are as per design changes and there is no duplicate parameter added.
- [ ]  Ensure removal of method/variable is covered properly along with usage removal.
- [ ]  Ensure code changes are as per design and requirement. Ensure change impact is assessed across the products.


### Reviewer checklist for this pull request
🚨 Reviewer Checklist (Mandatory) :

- [ ]  The Git branching and commit message guidelines are followed.
- [ ]  There are no conflicts in the pull request and pull is taken from origin branch.
- [ ]  A good naming convention is followed for variables, constants, functions, classes, and packages as defined in
    
    | # | Guideline (GDL)                       |
    |:-:|:--------------------------------------|
    | 1 | GDL-005 Coding Standards – Java       |
    | 2 | GDL-006 Coding Standards – JavaScript |
    | 3 | GDL-007 Coding Standards - PLSQL      |

- [ ]  The proper logging information is added within method(s) (info, error, or debug).
- [ ]  The code styling, code alignment, proper spacing, code comments, and error handling is ensured. The best and proper data type is used instead of using 'def' (in groovy) or any generic data type.
- [ ]  The code additions are pass linting checks and unit tests, ensuring sufficient coverage to prevent disruptions to existing functionality.
- [ ]  The unit test(s) is/are added/updated as per requirement and design.
- [ ]  The DRY principle is followed.
- [ ]  The code performance is reviewed where loops/threads are involved, and evidence(s) is attached to the PR when required.
- [ ]  New configurable parameter(s) is/are as per design changes and there is no duplicate parameter.
- [ ]  The removal of method/variable is covered properly along with usage removal.
- [ ]  The code changes including UX/UI is/are as per design and requirement.
- [ ]  The security review is performed and is in alignment with “WI-008 Secure Coding Practices”.
- [ ]  The change impact is assessed across the products.