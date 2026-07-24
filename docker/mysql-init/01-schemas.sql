-- Runs once, on first container start (docker-entrypoint-initdb.d convention).
-- One schema per service — see docs/architecture/02-service-topology.md.
CREATE DATABASE IF NOT EXISTS inventory_db;
CREATE DATABASE IF NOT EXISTS wallet_db;
CREATE DATABASE IF NOT EXISTS shop_db;
