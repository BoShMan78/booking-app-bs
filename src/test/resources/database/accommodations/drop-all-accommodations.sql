DELETE FROM accommodation_amenities;
DELETE FROM accommodations;
DELETE FROM addresses;
ALTER TABLE accommodations ALTER COLUMN id RESTART WITH 1;
