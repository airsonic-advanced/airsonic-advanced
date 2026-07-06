# Assessment Overview

This directory contains supplementary architecture and design documentation generated as part of the application assessment for Airsonic Advanced.

## Available Documents

| Document | Description |
|----------|-------------|
| [architecture-diagram.md](./architecture-diagram.md) | Application architecture visualization with Mermaid diagrams showing layers, components, data flow, and technology stack summary |
| [dependency-map.md](./dependency-map.md) | External dependency map organized by functional category with version details and Mermaid visualization |
| [api-service-contracts.md](./api-service-contracts.md) | API endpoints inventory, service catalog, communication patterns, DTOs, and sequence diagrams |
| [data-architecture.md](./data-architecture.md) | Data model with ER diagrams, entity details, data access patterns, database configuration, and migration strategy |
| [configuration-inventory.md](./configuration-inventory.md) | Complete configuration inventory covering application properties, Spring profiles, security config, and infrastructure settings |
| [business-workflows.md](./business-workflows.md) | Core business workflows with sequence diagrams, service layer design, transaction management, and validation rules |

## Summary

The Airsonic Advanced application is a Java 21 / Spring Boot 3.3.7 multi-module Maven project serving as a music streaming server. The assessment covers:

- **4 Maven modules**: airsonic-main (core), airsonic-sonos-api, subsonic-rest-api, integration-test
- **70+ external dependencies** across web frameworks, database, caching, security, media processing, and utilities
- **75+ REST API endpoints** plus SOAP (Sonos), WebSocket (STOMP), and UPnP/DLNA interfaces
- **~30 database tables** with Liquibase-managed schema evolution
- **11 business domains** including media scanning, streaming, user management, and podcast support
- **19 configuration sources** spanning properties files, Java config classes, and Docker configuration
