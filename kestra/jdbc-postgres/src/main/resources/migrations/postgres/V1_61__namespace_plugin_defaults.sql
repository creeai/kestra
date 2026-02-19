CREATE TABLE IF NOT EXISTS namespace_plugin_defaults (
    key VARCHAR(500) NOT NULL PRIMARY KEY,
    value JSONB NOT NULL
);
