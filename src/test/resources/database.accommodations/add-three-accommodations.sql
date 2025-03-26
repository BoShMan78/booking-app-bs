INSERT INTO addresses (country, city, street, house, apartment) VALUES
                                                                    ('Ukraine', 'Odesa', 'Deribasovskaya', '1', 1),
                                                                    ('Ukraine', 'Kyiv', 'Khreshchatyk', '2', NULL),
                                                                    ('Ukraine', 'Lviv', 'Rynok Square', '4', NULL),

SELECT @address_id_1 := (SELECT id FROM addresses WHERE country = 'Ukraine' AND city = 'Odesa' AND street = 'Deribasovskaya' LIMIT 1);
SELECT @address_id_2 := (SELECT id FROM addresses WHERE country = 'Ukraine' AND city = 'Kyiv' AND street = 'Khreshchatyk' LIMIT 1);
SELECT @address_id_3 := (SELECT id FROM addresses WHERE country = 'Ukraine' AND city = 'Lviv' AND street = 'Rynok Square' LIMIT 1);

INSERT INTO accommodations (type, address_id, size, daily_rate, availability) VALUES
                                                                                  ('APARTMENT', @address_id_1, '1 bedroom', 55.00, 10),
                                                                                  ('HOUSE', @address_id_2, '3 bedrooms', 120.50, 5),
                                                                                  ('CONDO', @address_id_3, '2 bedrooms', 80.00, 2);