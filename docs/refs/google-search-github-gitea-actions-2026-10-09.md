# Google search: GitHub/Gitea Actions compatibility

- Retrieval date: 2026-10-09
- Search engine: Google
- Search URL: <https://www.google.com/search?q=site%3Adocs.github.com+GitHub+Actions+workflow_dispatch+environment+required+reviewers>
- Search URL: <https://www.google.com/search?q=site%3Adocs.gitea.com+Actions+workflow+compatibility+runner>

## Official sources

- GitHub workflow syntax: <https://docs.github.com/en/actions/reference/workflows-and-actions/workflow-syntax>
- GitHub deployment environments: <https://docs.github.com/en/actions/deployment/targeting-different-environments/using-environments-for-deployment>
- Gitea Actions overview: <https://docs.gitea.com/usage/actions/overview>
- Gitea Actions compatibility: <https://docs.gitea.com/usage/actions/comparison>
- Gitea Actions quick start: <https://docs.gitea.com/usage/actions/quickstart>
- Gitea Actions variables: <https://docs.gitea.com/usage/actions/actions-variables>

## Findings used by this repository

1. GitHub workflow files live under `.github/workflows`; Gitea workflow files are uploaded under `.gitea/workflows`.
2. Gitea Actions is designed to be mostly compatible with GitHub Actions and uses an independent Gitea Runner.
3. `push`, `pull_request`, and `workflow_dispatch` are suitable common workflow triggers for this design.
4. Gitea ignores `jobs.<job_id>.environment`, so release approval cannot depend on GitHub-only environment protection. The release workflow therefore requires a manual dispatch and an explicit `confirm_release=true` input in both hosts.
5. Gitea exposes GitHub-compatible contexts and supports repository variables and secrets. The workflows use `vars.RELEASE_*` for host/repository configuration and `secrets.RELEASE_TOKEN` for the publishing credential.
6. Gitea's compatibility page documents differences in token permissions and action downloading, so the workflow keeps the job command surface simple and uses the same checked-in shell scripts on both hosts.
