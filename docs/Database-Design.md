# Data Design

[English](Database-Design.md) · [简体中文](Database-Design.zh-CN.md)

The current prototype uses Android SharedPreferences for scalar settings and token records, with a process-local lock around token updates. Before LAN release, migrate token and audit records to an atomic repository with the retention and concurrency guarantees below; do not treat the prototype store as a release-grade database.

## `settings`

| Column | Type | Rule |
| --- | --- | --- |
| `key` | text | primary key |
| `value` | text | serialized scalar; never token secret |

Required keys: `mcp_enabled`, `bind_mode`, `bind_host`, `port`, `target_sdk`, `debug_enabled`.

## `tokens`

| Column | Type | Rule |
| --- | --- | --- |
| `id` | text | random opaque id, primary key |
| `verifier` | blob/text | salted verifier, never raw token |
| `mode` | text | `one_time`, `reusable`, `fixed_duration`, `unlimited` |
| `created_at` | integer | UTC epoch milliseconds |
| `expires_at` | integer nullable | required for fixed duration |
| `enabled_tools` | JSON array | tool-name allow-list for this token; defaults to read-only tools and must not include unknown tools |
| `use_count` | integer | atomic increment |
| `last_used_at` | integer nullable | metadata only |

The process permission baseline is read-only. Tool names that can launch applications, inject input, or execute shell commands are not enabled by default and require explicit per-token selection. `shell_exec` is always disabled for newly created tokens unless the operator turns it on for that token.

Deleting a token removes the complete record, including its verifier. The data model has no `revoked_at`, `revoked`, or tombstone field. A deleted secret authenticates exactly like an unknown secret, and a deleted token is absent from listing and audit references.

## `audit_events`

Store event id, time, action, token id (not secret), tool name, outcome, error code, duration, and redacted request summary. Do not store command text, input text, screenshots, token values, or full UI content by default.

## Retention

Keep at most 500 audit events and delete oldest entries transactionally. Delete token records explicitly; expired records may be pruned only after they are no longer needed for operator audit display.
