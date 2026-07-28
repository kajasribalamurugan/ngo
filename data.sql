INSERT INTO campaigns (name, description, goal_amount)
SELECT 'Healthy Children', 'Medical support and nutrition kits for children.', 150000
WHERE NOT EXISTS (SELECT 1 FROM campaigns WHERE name = 'Healthy Children');
INSERT INTO campaigns (name, description, goal_amount)
SELECT 'Back to School', 'Books, uniforms and learning materials for students.', 100000
WHERE NOT EXISTS (SELECT 1 FROM campaigns WHERE name = 'Back to School');

INSERT INTO donations (campaign_id, donor_name, amount, donated_on)
SELECT 1, 'Aarav Sharma', 25000, '2026-07-02' WHERE NOT EXISTS (SELECT 1 FROM donations WHERE donor_name='Aarav Sharma' AND donated_on='2026-07-02');
INSERT INTO donations (campaign_id, donor_name, amount, donated_on)
SELECT 1, 'Anonymous', 18000, '2026-07-08' WHERE NOT EXISTS (SELECT 1 FROM donations WHERE amount=18000 AND donated_on='2026-07-08');
INSERT INTO donations (campaign_id, donor_name, amount, donated_on)
SELECT 1, 'Nisha Patel', 12000, '2026-07-15' WHERE NOT EXISTS (SELECT 1 FROM donations WHERE donor_name='Nisha Patel' AND donated_on='2026-07-15');
INSERT INTO donations (campaign_id, donor_name, amount, donated_on)
SELECT 2, 'Rahul Mehta', 30000, '2026-07-05' WHERE NOT EXISTS (SELECT 1 FROM donations WHERE donor_name='Rahul Mehta' AND donated_on='2026-07-05');
INSERT INTO donations (campaign_id, donor_name, amount, donated_on)
SELECT 2, 'Anonymous', 14000, '2026-07-19' WHERE NOT EXISTS (SELECT 1 FROM donations WHERE amount=14000 AND donated_on='2026-07-19');

INSERT INTO expenses (campaign_id, item_name, category, amount, spent_on)
SELECT 1, 'Nutrition kits', 'Medical', 16000, '2026-07-10' WHERE NOT EXISTS (SELECT 1 FROM expenses WHERE item_name='Nutrition kits');
INSERT INTO expenses (campaign_id, item_name, category, amount, spent_on)
SELECT 1, 'Clinic medicines', 'Medical', 11000, '2026-07-18' WHERE NOT EXISTS (SELECT 1 FROM expenses WHERE item_name='Clinic medicines');
INSERT INTO expenses (campaign_id, item_name, category, amount, spent_on)
SELECT 1, 'Transport to village', 'Admin', 3000, '2026-07-20' WHERE NOT EXISTS (SELECT 1 FROM expenses WHERE item_name='Transport to village');
INSERT INTO expenses (campaign_id, item_name, category, amount, spent_on)
SELECT 2, 'School books', 'Education', 18000, '2026-07-11' WHERE NOT EXISTS (SELECT 1 FROM expenses WHERE item_name='School books');
INSERT INTO expenses (campaign_id, item_name, category, amount, spent_on)
SELECT 2, 'Student uniforms', 'Education', 9000, '2026-07-21' WHERE NOT EXISTS (SELECT 1 FROM expenses WHERE item_name='Student uniforms');
INSERT INTO expenses (campaign_id, item_name, category, amount, spent_on)
SELECT 2, 'Printing receipts', 'Admin', 1500, '2026-07-22' WHERE NOT EXISTS (SELECT 1 FROM expenses WHERE item_name='Printing receipts');
