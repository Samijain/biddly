-- Development-only sample data, loaded only when the "dev" profile is active.
-- "R__" = repeatable migration: Flyway re-runs it whenever this file's checksum changes,
-- so every statement must be safe to run more than once.

INSERT INTO users (display_name, email) VALUES
    ('Asha Seller',  'asha@biddly.dev'),
    ('Rohan Bidder', 'rohan@biddly.dev'),
    ('Meera Bidder', 'meera@biddly.dev')
ON CONFLICT ((lower(email))) DO NOTHING;

INSERT INTO auctions (seller_id, category_id, title, description, cover_image_url,
                      starting_price, current_price, minimum_increment,
                      start_time, end_time, status)
SELECT u.id, c.id, s.title, s.description, s.cover_image_url,
       s.starting_price, s.starting_price, s.minimum_increment,
       now() - interval '1 hour', now() + s.duration, s.status
FROM (VALUES
    ('Vintage Leica M3 Camera',        'collectibles', 'Fully serviced 1956 rangefinder with original leather case.', 'https://picsum.photos/seed/leica/800/600',     45000.00, 1000.00, interval '2 days',    'ACTIVE'),
    ('Sony WH-1000XM5 Headphones',     'electronics',  'Sealed box, Indian warranty valid for 12 months.',           'https://picsum.photos/seed/headphones/800/600', 18000.00,  500.00, interval '5 hours',   'ACTIVE'),
    ('Hand-painted Madhubani Canvas',  'art',          'Original 24x36 inch canvas by a Bihar folk artist.',         'https://picsum.photos/seed/madhubani/800/600',   8500.00,  250.00, interval '3 days',    'ACTIVE'),
    ('Signed Cricket Bat',             'sports',       'English willow bat signed by the 2011 World Cup squad.',     'https://picsum.photos/seed/cricket/800/600',    60000.00, 2000.00, interval '40 minutes','ACTIVE'),
    ('Teak Wood Coffee Table',         'home-living',  'Solid Burma teak, hand-finished, 4 ft x 2 ft.',              'https://picsum.photos/seed/teak/800/600',       22000.00,  500.00, interval '1 day',     'ACTIVE'),
    ('Banarasi Silk Saree',            'fashion',      'Pure katan silk with real zari border.',                     'https://picsum.photos/seed/saree/800/600',      15000.00,  500.00, interval '6 days',    'DRAFT')
) AS s(title, category_slug, description, cover_image_url, starting_price, minimum_increment, duration, status)
JOIN categories c ON c.slug = s.category_slug
JOIN users u ON lower(u.email) = 'asha@biddly.dev'
WHERE NOT EXISTS (SELECT 1 FROM auctions);
