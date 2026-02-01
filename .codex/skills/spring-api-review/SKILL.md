---
name: spring-api-review
description: Review Spring Boot REST changes for correctness, transaction boundaries, and QueryDSL/JPA performance pitfalls.
---

## Use when
- After changing Controller/Service/Repository
- Before PR

## Checklist
1) Validate request/response DTOs and error responses.
2) Check transactional boundaries and lazy loading/N+1.
3) Check QueryDSL predicates, joins, pagination count query.
4) Suggest minimal safe fixes + tests to add.

