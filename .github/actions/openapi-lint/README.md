# OpenAPI lint

Lints an OpenAPI spec with [Spectral](https://github.com/stoplightio/spectral) and fails the job when the findings exceed the configured thresholds. Replaces `zuplo/rmoa-action`, which depends on a cloud API key and only exposes numeric thresholds, with no way to disable a single rule.

The rules live in the repository, in `.spectral.yaml`.

## Usage

```yaml
- uses: actions/checkout@34e114876b0b11c390a56381ad16ebd13914f8d5 # v4

- name: OpenAPI lint - ${{ matrix.spec.version }}
  uses: ./.github/actions/openapi-lint
  with:
    spec_path: ${{ matrix.spec.path }}
```

The spec matrix stays in the calling workflow, because the number of specs differs per repository.

## Inputs

| Input | Required | Default | Description |
| --- | --- | --- | --- |
| `spec_path` | yes | | Path to the OpenAPI spec, relative to the repository root |
| `ruleset` | no | `.spectral.yaml` | Path to the Spectral ruleset |
| `max_errors` | no | `0` | Error findings tolerated before the job fails |
| `max_warnings` | no | `0` | Warning findings tolerated before the job fails |
| `fail_on_violation` | no | `true` | Set to `false` for a report-only rollout |
| `spectral_version` | no | `6.16.3` | Version of `@stoplight/spectral-cli` |
| `owasp_ruleset_version` | no | `2.0.1` | Version of `@stoplight/spectral-owasp-ruleset` |

## Behaviour worth knowing

**A lint that cannot run fails the job, in report-only mode too.** Spectral exits `1` when it finds results but `2` or more when it could not run at all, and in that case it writes no report. A gate that does not tell the two apart passes silently and stops protecting anything. An unloadable ruleset is a configuration error, not a finding, so `fail_on_violation: false` does not suppress it.

**Only errors and warnings are annotated on the pull request.** GitHub renders at most 10 annotations per type per step, so annotating info findings as well would push the actionable ones out of the view. Info findings are reported as counts, broken down by rule, in the job summary.

**The npm packages are installed next to the ruleset.** Spectral resolves the packages a ruleset extends starting from the directory of the ruleset file, and these repositories have no `package.json`. The install therefore targets `dirname(ruleset)` and creates only a `node_modules` directory, which is already ignored by git.
