INSERT INTO addresses (country, city, street, house, apartment, is_deleted) VALUES
                                                                    ('Ukraine', 'Odesa', 'Deribasovskaya', '1', 1, FALSE),
                                                                    ('Ukraine', 'Kyiv', 'Khreshchatyk', '2', NULL, FALSE),
                                                                    ('Ukraine', 'Lviv', 'Rynok Square', '4', NULL, FALSE);

INSERT INTO accommodations (type, address_id, size, daily_rate, availability, is_deleted) VALUES
    ('APARTMENT', (SELECT id FROM addresses WHERE country = 'Ukraine' AND city = 'Odesa' AND street = 'Deribasovskaya' LIMIT 1), '1 bedroom', 55.00, 10, FALSE),
    ('HOUSE', (SELECT id FROM addresses WHERE country = 'Ukraine' AND city = 'Kyiv' AND street = 'Khreshchatyk' LIMIT 1), '3 bedrooms', 120.50, 5, FALSE),
    ('CONDO', (SELECT id FROM addresses WHERE country = 'Ukraine' AND city = 'Lviv' AND street = 'Rynok Square' LIMIT 1), '2 bedrooms', 80.00, 2, FALSE);
