# AI Service Platform

## Vision

Build a modular AI ecosystem where multiple applications share a common authentication platform while remaining independently deployable.

---

## Workspace Structure

platform/
    identity-service
    ai-platform
    chat-platform

applications/
    insighttube/
        frontend
        gateway
        backend

---

## Platform Services

Platform services provide reusable capabilities.

Current platform services

- Identity Service
- AI Platform (future)
- Chat Platform (future)

Platform services NEVER contain application-specific business logic.

---

## Application Services

Each application owns:

- frontend
- gateway
- backend

Each application is independently deployable.

---

## Authentication

Authentication is handled ONLY by Identity Service.

Responsibilities

- Registration
- Login
- JWT
- Refresh Tokens
- Authorization
- Roles
- Sessions

Identity NEVER stores:

- profile
- youtube data
- business data

---

## Application Identity

Applications authenticate users using JWTs issued by Identity.

Applications never generate JWTs.

Applications never manage passwords.

---

## Current Application

InsightTube

Purpose

AI powered YouTube productivity platform.

Architecture

Frontend
↓

Gateway
↓

Backend
↓

Platform Services