import re

# Update CheckInResourceTest (it uses actual database via testcontainers so we don't mock)
# BUT wait! We should verify if CheckInResourceTest uses mocking (Panache mock) or a real DB.
# Let's check imports of CheckInResourceTest.
