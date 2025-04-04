INSERT INTO bookings (check_in_date, check_out_date, accommodation_id, user_id, status, is_deleted) VALUES
    ('2027-04-05', '2027-04-10', (SELECT id FROM accommodations WHERE size = '1 bedroom' LIMIT 1), 1, 'PENDING', FALSE),
    ('2027-04-12', '2027-04-15', (SELECT id FROM accommodations WHERE size = '3 bedrooms' LIMIT 1), 1, 'CONFIRMED', FALSE),
    ('2027-04-18', '2027-04-20', (SELECT id FROM accommodations WHERE size = '2 bedrooms' LIMIT 1), 1, 'PENDING', FALSE);