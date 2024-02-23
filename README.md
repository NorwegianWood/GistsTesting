# GistsTesting

You need to add your own credentials to test/resources/base.properties: your GitHub token and GitHub user's name.
You can generate your token here: https://github.com/settings/tokens.

This project uses Allure (https://allurereport.org/docs/) for reporting. To see the report you need to do the following:

install allure (you can find instructions here: https://allurereport.org/docs/gettingstarted-installation/).
run tests (run 'mvn clean test' command) in the project folder.
run 'allure serve allure-results' command in the same folder.
