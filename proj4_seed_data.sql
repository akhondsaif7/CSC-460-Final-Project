-- =========================================================
-- SEED DATA FOR 460 PROJECT 4
-- Run after 460-p4.sql.
-- Inserts at least 15 rows into every table.
-- =========================================================

-- -------------------------
-- Plan: 15 rows
-- -------------------------
INSERT INTO Plan (plan, max_daily_msg, pro_model, cost) VALUES ('Free', 25, 0, 0);
INSERT INTO Plan (plan, max_daily_msg, pro_model, cost) VALUES ('Starter', 100, 0, 9);
INSERT INTO Plan (plan, max_daily_msg, pro_model, cost) VALUES ('Basic', 250, 0, 15);
INSERT INTO Plan (plan, max_daily_msg, pro_model, cost) VALUES ('Plus', 500, 1, 20);
INSERT INTO Plan (plan, max_daily_msg, pro_model, cost) VALUES ('Pro', 1000, 1, 35);
INSERT INTO Plan (plan, max_daily_msg, pro_model, cost) VALUES ('Team', 2000, 1, 60);
INSERT INTO Plan (plan, max_daily_msg, pro_model, cost) VALUES ('Business', 5000, 1, 120);
INSERT INTO Plan (plan, max_daily_msg, pro_model, cost) VALUES ('Enterprise', 10000, 1, 300);
INSERT INTO Plan (plan, max_daily_msg, pro_model, cost) VALUES ('Student', 150, 0, 5);
INSERT INTO Plan (plan, max_daily_msg, pro_model, cost) VALUES ('Research', 3000, 1, 80);
INSERT INTO Plan (plan, max_daily_msg, pro_model, cost) VALUES ('Creator', 750, 1, 25);
INSERT INTO Plan (plan, max_daily_msg, pro_model, cost) VALUES ('Developer', 1500, 1, 40);
INSERT INTO Plan (plan, max_daily_msg, pro_model, cost) VALUES ('Agency', 4000, 1, 99);
INSERT INTO Plan (plan, max_daily_msg, pro_model, cost) VALUES ('Education', 1200, 1, 30);
INSERT INTO Plan (plan, max_daily_msg, pro_model, cost) VALUES ('Trial', 50, 0, 0);

-- -------------------------
-- User: 15 rows
-- -------------------------
INSERT INTO "User" (UsrID, full_name, email, created_at, preferred_language, plan) VALUES (1, 'Ava Martinez', 'ava.martinez@example.com', TIMESTAMP '2026-01-02 09:00:00', 'English', 'Free');
INSERT INTO "User" (UsrID, full_name, email, created_at, preferred_language, plan) VALUES (2, 'Liam Johnson', 'liam.johnson@example.com', TIMESTAMP '2026-01-03 09:30:00', 'English', 'Starter');
INSERT INTO "User" (UsrID, full_name, email, created_at, preferred_language, plan) VALUES (3, 'Sofia Garcia', 'sofia.garcia@example.com', TIMESTAMP '2026-01-04 10:00:00', 'Spanish', 'Basic');
INSERT INTO "User" (UsrID, full_name, email, created_at, preferred_language, plan) VALUES (4, 'Noah Brown', 'noah.brown@example.com', TIMESTAMP '2026-01-05 10:30:00', 'English', 'Plus');
INSERT INTO "User" (UsrID, full_name, email, created_at, preferred_language, plan) VALUES (5, 'Mia Davis', 'mia.davis@example.com', TIMESTAMP '2026-01-06 11:00:00', 'French', 'Pro');
INSERT INTO "User" (UsrID, full_name, email, created_at, preferred_language, plan) VALUES (6, 'Ethan Wilson', 'ethan.wilson@example.com', TIMESTAMP '2026-01-07 11:30:00', 'English', 'Team');
INSERT INTO "User" (UsrID, full_name, email, created_at, preferred_language, plan) VALUES (7, 'Isabella Lee', 'isabella.lee@example.com', TIMESTAMP '2026-01-08 12:00:00', 'Korean', 'Business');
INSERT INTO "User" (UsrID, full_name, email, created_at, preferred_language, plan) VALUES (8, 'Lucas Smith', 'lucas.smith@example.com', TIMESTAMP '2026-01-09 12:30:00', 'English', 'Enterprise');
INSERT INTO "User" (UsrID, full_name, email, created_at, preferred_language, plan) VALUES (9, 'Camila Rodriguez', 'camila.rodriguez@example.com', TIMESTAMP '2026-01-10 13:00:00', 'Spanish', 'Student');
INSERT INTO "User" (UsrID, full_name, email, created_at, preferred_language, plan) VALUES (10, 'Benjamin Moore', 'benjamin.moore@example.com', TIMESTAMP '2026-01-11 13:30:00', 'English', 'Research');
INSERT INTO "User" (UsrID, full_name, email, created_at, preferred_language, plan) VALUES (11, 'Olivia Taylor', 'olivia.taylor@example.com', TIMESTAMP '2026-01-12 14:00:00', 'Italian', 'Creator');
INSERT INTO "User" (UsrID, full_name, email, created_at, preferred_language, plan) VALUES (12, 'James Anderson', 'james.anderson@example.com', TIMESTAMP '2026-01-13 14:30:00', 'English', 'Developer');
INSERT INTO "User" (UsrID, full_name, email, created_at, preferred_language, plan) VALUES (13, 'Emma Thomas', 'emma.thomas@example.com', TIMESTAMP '2026-01-14 15:00:00', 'German', 'Agency');
INSERT INTO "User" (UsrID, full_name, email, created_at, preferred_language, plan) VALUES (14, 'Daniel Hernandez', 'daniel.hernandez@example.com', TIMESTAMP '2026-01-15 15:30:00', 'Spanish', 'Education');
INSERT INTO "User" (UsrID, full_name, email, created_at, preferred_language, plan) VALUES (15, 'Charlotte White', 'charlotte.white@example.com', TIMESTAMP '2026-01-16 16:00:00', 'English', 'Trial');

-- -------------------------
-- Workspace: 15 rows
-- Note: Java workspace flows ensure owner rows exist in WorkspaceMember.
-- -------------------------
INSERT INTO Workspace (wsID, owner, created_at, preferred_language, plan) VALUES (101, 1, TIMESTAMP '2026-02-01 09:00:00', 'English', 'Free');
INSERT INTO Workspace (wsID, owner, created_at, preferred_language, plan) VALUES (102, 2, TIMESTAMP '2026-02-02 09:00:00', 'English', 'Starter');
INSERT INTO Workspace (wsID, owner, created_at, preferred_language, plan) VALUES (103, 3, TIMESTAMP '2026-02-03 09:00:00', 'Spanish', 'Basic');
INSERT INTO Workspace (wsID, owner, created_at, preferred_language, plan) VALUES (104, 4, TIMESTAMP '2026-02-04 09:00:00', 'English', 'Plus');
INSERT INTO Workspace (wsID, owner, created_at, preferred_language, plan) VALUES (105, 5, TIMESTAMP '2026-02-05 09:00:00', 'French', 'Pro');
INSERT INTO Workspace (wsID, owner, created_at, preferred_language, plan) VALUES (106, 6, TIMESTAMP '2026-02-06 09:00:00', 'English', 'Team');
INSERT INTO Workspace (wsID, owner, created_at, preferred_language, plan) VALUES (107, 7, TIMESTAMP '2026-02-07 09:00:00', 'Korean', 'Business');
INSERT INTO Workspace (wsID, owner, created_at, preferred_language, plan) VALUES (108, 8, TIMESTAMP '2026-02-08 09:00:00', 'English', 'Enterprise');
INSERT INTO Workspace (wsID, owner, created_at, preferred_language, plan) VALUES (109, 9, TIMESTAMP '2026-02-09 09:00:00', 'Spanish', 'Student');
INSERT INTO Workspace (wsID, owner, created_at, preferred_language, plan) VALUES (110, 10, TIMESTAMP '2026-02-10 09:00:00', 'English', 'Research');
INSERT INTO Workspace (wsID, owner, created_at, preferred_language, plan) VALUES (111, 11, TIMESTAMP '2026-02-11 09:00:00', 'Italian', 'Creator');
INSERT INTO Workspace (wsID, owner, created_at, preferred_language, plan) VALUES (112, 12, TIMESTAMP '2026-02-12 09:00:00', 'English', 'Developer');
INSERT INTO Workspace (wsID, owner, created_at, preferred_language, plan) VALUES (113, 13, TIMESTAMP '2026-02-13 09:00:00', 'German', 'Agency');
INSERT INTO Workspace (wsID, owner, created_at, preferred_language, plan) VALUES (114, 14, TIMESTAMP '2026-02-14 09:00:00', 'Spanish', 'Education');
INSERT INTO Workspace (wsID, owner, created_at, preferred_language, plan) VALUES (115, 15, TIMESTAMP '2026-02-15 09:00:00', 'English', 'Trial');

-- -------------------------
-- WorkspaceMember: 30 rows
-- First 15 rows insert each workspace owner manually.
-- Next 15 rows add non-owner members.
-- -------------------------
INSERT INTO WorkspaceMember (wsID, UsrID, role, joined_at) VALUES (101, 1, 'owner', TIMESTAMP '2026-02-01 09:00:00');
INSERT INTO WorkspaceMember (wsID, UsrID, role, joined_at) VALUES (102, 2, 'owner', TIMESTAMP '2026-02-02 09:00:00');
INSERT INTO WorkspaceMember (wsID, UsrID, role, joined_at) VALUES (103, 3, 'owner', TIMESTAMP '2026-02-03 09:00:00');
INSERT INTO WorkspaceMember (wsID, UsrID, role, joined_at) VALUES (104, 4, 'owner', TIMESTAMP '2026-02-04 09:00:00');
INSERT INTO WorkspaceMember (wsID, UsrID, role, joined_at) VALUES (105, 5, 'owner', TIMESTAMP '2026-02-05 09:00:00');
INSERT INTO WorkspaceMember (wsID, UsrID, role, joined_at) VALUES (106, 6, 'owner', TIMESTAMP '2026-02-06 09:00:00');
INSERT INTO WorkspaceMember (wsID, UsrID, role, joined_at) VALUES (107, 7, 'owner', TIMESTAMP '2026-02-07 09:00:00');
INSERT INTO WorkspaceMember (wsID, UsrID, role, joined_at) VALUES (108, 8, 'owner', TIMESTAMP '2026-02-08 09:00:00');
INSERT INTO WorkspaceMember (wsID, UsrID, role, joined_at) VALUES (109, 9, 'owner', TIMESTAMP '2026-02-09 09:00:00');
INSERT INTO WorkspaceMember (wsID, UsrID, role, joined_at) VALUES (110, 10, 'owner', TIMESTAMP '2026-02-10 09:00:00');
INSERT INTO WorkspaceMember (wsID, UsrID, role, joined_at) VALUES (111, 11, 'owner', TIMESTAMP '2026-02-11 09:00:00');
INSERT INTO WorkspaceMember (wsID, UsrID, role, joined_at) VALUES (112, 12, 'owner', TIMESTAMP '2026-02-12 09:00:00');
INSERT INTO WorkspaceMember (wsID, UsrID, role, joined_at) VALUES (113, 13, 'owner', TIMESTAMP '2026-02-13 09:00:00');
INSERT INTO WorkspaceMember (wsID, UsrID, role, joined_at) VALUES (114, 14, 'owner', TIMESTAMP '2026-02-14 09:00:00');
INSERT INTO WorkspaceMember (wsID, UsrID, role, joined_at) VALUES (115, 15, 'owner', TIMESTAMP '2026-02-15 09:00:00');
INSERT INTO WorkspaceMember (wsID, UsrID, role, joined_at) VALUES (101, 2, 'admin', TIMESTAMP '2026-02-01 10:00:00');
INSERT INTO WorkspaceMember (wsID, UsrID, role, joined_at) VALUES (102, 3, 'member', TIMESTAMP '2026-02-02 10:00:00');
INSERT INTO WorkspaceMember (wsID, UsrID, role, joined_at) VALUES (103, 4, 'viewer', TIMESTAMP '2026-02-03 10:00:00');
INSERT INTO WorkspaceMember (wsID, UsrID, role, joined_at) VALUES (104, 5, 'admin', TIMESTAMP '2026-02-04 10:00:00');
INSERT INTO WorkspaceMember (wsID, UsrID, role, joined_at) VALUES (105, 6, 'member', TIMESTAMP '2026-02-05 10:00:00');
INSERT INTO WorkspaceMember (wsID, UsrID, role, joined_at) VALUES (106, 7, 'viewer', TIMESTAMP '2026-02-06 10:00:00');
INSERT INTO WorkspaceMember (wsID, UsrID, role, joined_at) VALUES (107, 8, 'admin', TIMESTAMP '2026-02-07 10:00:00');
INSERT INTO WorkspaceMember (wsID, UsrID, role, joined_at) VALUES (108, 9, 'member', TIMESTAMP '2026-02-08 10:00:00');
INSERT INTO WorkspaceMember (wsID, UsrID, role, joined_at) VALUES (109, 10, 'viewer', TIMESTAMP '2026-02-09 10:00:00');
INSERT INTO WorkspaceMember (wsID, UsrID, role, joined_at) VALUES (110, 11, 'admin', TIMESTAMP '2026-02-10 10:00:00');
INSERT INTO WorkspaceMember (wsID, UsrID, role, joined_at) VALUES (111, 12, 'member', TIMESTAMP '2026-02-11 10:00:00');
INSERT INTO WorkspaceMember (wsID, UsrID, role, joined_at) VALUES (112, 13, 'viewer', TIMESTAMP '2026-02-12 10:00:00');
INSERT INTO WorkspaceMember (wsID, UsrID, role, joined_at) VALUES (113, 14, 'admin', TIMESTAMP '2026-02-13 10:00:00');
INSERT INTO WorkspaceMember (wsID, UsrID, role, joined_at) VALUES (114, 15, 'member', TIMESTAMP '2026-02-14 10:00:00');
INSERT INTO WorkspaceMember (wsID, UsrID, role, joined_at) VALUES (115, 1, 'viewer', TIMESTAMP '2026-02-15 10:00:00');

-- -------------------------
-- PaymentMethod: 15 rows
-- -------------------------
INSERT INTO PaymentMethod (payment_method_ID, UsrID, type, billing_addr, status) VALUES (201, 1, 'Visa', '100 Main St, Tucson, AZ', 'active');
INSERT INTO PaymentMethod (payment_method_ID, UsrID, type, billing_addr, status) VALUES (202, 2, 'Mastercard', '101 Main St, Phoenix, AZ', 'active');
INSERT INTO PaymentMethod (payment_method_ID, UsrID, type, billing_addr, status) VALUES (203, 3, 'Amex', '102 Main St, Austin, TX', 'active');
INSERT INTO PaymentMethod (payment_method_ID, UsrID, type, billing_addr, status) VALUES (204, 4, 'Visa', '103 Main St, Dallas, TX', 'inactive');
INSERT INTO PaymentMethod (payment_method_ID, UsrID, type, billing_addr, status) VALUES (205, 5, 'Bank Account', '104 Main St, Raleigh, NC', 'active');
INSERT INTO PaymentMethod (payment_method_ID, UsrID, type, billing_addr, status) VALUES (206, 6, 'Mastercard', '105 Main St, Charlotte, NC', 'expired');
INSERT INTO PaymentMethod (payment_method_ID, UsrID, type, billing_addr, status) VALUES (207, 7, 'Visa', '106 Main St, Atlanta, GA', 'active');
INSERT INTO PaymentMethod (payment_method_ID, UsrID, type, billing_addr, status) VALUES (208, 8, 'Amex', '107 Main St, Savannah, GA', 'active');
INSERT INTO PaymentMethod (payment_method_ID, UsrID, type, billing_addr, status) VALUES (209, 9, 'Visa', '108 Main St, Nashville, TN', 'archived');
INSERT INTO PaymentMethod (payment_method_ID, UsrID, type, billing_addr, status) VALUES (210, 10, 'Mastercard', '109 Main St, Memphis, TN', 'active');
INSERT INTO PaymentMethod (payment_method_ID, UsrID, type, billing_addr, status) VALUES (211, 11, 'Bank Account', '110 Main St, Denver, CO', 'active');
INSERT INTO PaymentMethod (payment_method_ID, UsrID, type, billing_addr, status) VALUES (212, 12, 'Visa', '111 Main St, Seattle, WA', 'inactive');
INSERT INTO PaymentMethod (payment_method_ID, UsrID, type, billing_addr, status) VALUES (213, 13, 'Mastercard', '112 Main St, Miami, FL', 'active');
INSERT INTO PaymentMethod (payment_method_ID, UsrID, type, billing_addr, status) VALUES (214, 14, 'Amex', '113 Main St, Chicago, IL', 'active');
INSERT INTO PaymentMethod (payment_method_ID, UsrID, type, billing_addr, status) VALUES (215, 15, 'Visa', '114 Main St, Boston, MA', 'deleted');

-- -------------------------
-- Invoice: 15 rows
-- -------------------------
INSERT INTO Invoice (InvNo, UsrID, amount, invoice_date, payment_method_ID, status) VALUES (301, 1, 0.00, DATE '2026-03-01', 201, 'paid');
INSERT INTO Invoice (InvNo, UsrID, amount, invoice_date, payment_method_ID, status) VALUES (302, 2, 9.00, DATE '2026-03-02', 202, 'open');
INSERT INTO Invoice (InvNo, UsrID, amount, invoice_date, payment_method_ID, status) VALUES (303, 3, 15.00, DATE '2026-03-03', 203, 'paid');
INSERT INTO Invoice (InvNo, UsrID, amount, invoice_date, payment_method_ID, status) VALUES (304, 4, 20.00, DATE '2026-03-04', 204, 'failed');
INSERT INTO Invoice (InvNo, UsrID, amount, invoice_date, payment_method_ID, status) VALUES (305, 5, 35.00, DATE '2026-03-05', 205, 'paid');
INSERT INTO Invoice (InvNo, UsrID, amount, invoice_date, payment_method_ID, status) VALUES (306, 6, 60.00, DATE '2026-03-06', 206, 'overdue');
INSERT INTO Invoice (InvNo, UsrID, amount, invoice_date, payment_method_ID, status) VALUES (307, 7, 120.00, DATE '2026-03-07', 207, 'open');
INSERT INTO Invoice (InvNo, UsrID, amount, invoice_date, payment_method_ID, status) VALUES (308, 8, 300.00, DATE '2026-03-08', 208, 'paid');
INSERT INTO Invoice (InvNo, UsrID, amount, invoice_date, payment_method_ID, status) VALUES (309, 9, 5.00, DATE '2026-03-09', 209, 'void');
INSERT INTO Invoice (InvNo, UsrID, amount, invoice_date, payment_method_ID, status) VALUES (310, 10, 80.00, DATE '2026-03-10', 210, 'paid');
INSERT INTO Invoice (InvNo, UsrID, amount, invoice_date, payment_method_ID, status) VALUES (311, 11, 25.00, DATE '2026-03-11', 211, 'refunded');
INSERT INTO Invoice (InvNo, UsrID, amount, invoice_date, payment_method_ID, status) VALUES (312, 12, 40.00, DATE '2026-03-12', 212, 'draft');
INSERT INTO Invoice (InvNo, UsrID, amount, invoice_date, payment_method_ID, status) VALUES (313, 13, 99.00, DATE '2026-03-13', 213, 'open');
INSERT INTO Invoice (InvNo, UsrID, amount, invoice_date, payment_method_ID, status) VALUES (314, 14, 30.00, DATE '2026-03-14', 214, 'paid');
INSERT INTO Invoice (InvNo, UsrID, amount, invoice_date, payment_method_ID, status) VALUES (315, 15, 0.00, DATE '2026-03-15', 215, 'paid');

-- -------------------------
-- Prompt: 15 rows
-- -------------------------
INSERT INTO Prompt (promptID, UsrID, wsID, name, text, category, created_at) VALUES (401, 1, 101, 'Study Helper', 'Explain this concept in simple terms.', 'Education', TIMESTAMP '2026-03-16 09:00:00');
INSERT INTO Prompt (promptID, UsrID, wsID, name, text, category, created_at) VALUES (402, 2, 102, 'Bug Finder', 'Review this code and find likely bugs.', 'Programming', TIMESTAMP '2026-03-16 09:15:00');
INSERT INTO Prompt (promptID, UsrID, wsID, name, text, category, created_at) VALUES (403, 3, 103, 'Spanish Rewrite', 'Rewrite this Spanish text clearly.', 'Writing', TIMESTAMP '2026-03-16 09:30:00');
INSERT INTO Prompt (promptID, UsrID, wsID, name, text, category, created_at) VALUES (404, 4, 104, 'SQL Explainer', 'Explain the SQL query step by step.', 'Database', TIMESTAMP '2026-03-16 09:45:00');
INSERT INTO Prompt (promptID, UsrID, wsID, name, text, category, created_at) VALUES (405, 5, 105, 'Email Draft', 'Draft a concise professional email.', 'Business', TIMESTAMP '2026-03-16 10:00:00');
INSERT INTO Prompt (promptID, UsrID, wsID, name, text, category, created_at) VALUES (406, 6, 106, 'Meeting Summary', 'Summarize this meeting into action items.', 'Productivity', TIMESTAMP '2026-03-16 10:15:00');
INSERT INTO Prompt (promptID, UsrID, wsID, name, text, category, created_at) VALUES (407, 7, 107, 'Marketing Ideas', 'Generate practical campaign ideas.', 'Marketing', TIMESTAMP '2026-03-16 10:30:00');
INSERT INTO Prompt (promptID, UsrID, wsID, name, text, category, created_at) VALUES (408, 8, 108, 'Policy Review', 'Identify risks in this policy.', 'Legal', TIMESTAMP '2026-03-16 10:45:00');
INSERT INTO Prompt (promptID, UsrID, wsID, name, text, category, created_at) VALUES (409, 9, 109, 'Math Tutor', 'Solve this problem and explain the logic.', 'Education', TIMESTAMP '2026-03-16 11:00:00');
INSERT INTO Prompt (promptID, UsrID, wsID, name, text, category, created_at) VALUES (410, 10, 110, 'Research Assistant', 'Extract the main claims from this paper.', 'Research', TIMESTAMP '2026-03-16 11:15:00');
INSERT INTO Prompt (promptID, UsrID, wsID, name, text, category, created_at) VALUES (411, 11, 111, 'Creative Brief', 'Turn these notes into a creative brief.', 'Creative', TIMESTAMP '2026-03-16 11:30:00');
INSERT INTO Prompt (promptID, UsrID, wsID, name, text, category, created_at) VALUES (412, 12, 112, 'API Planner', 'Design simple API routes for this feature.', 'Programming', TIMESTAMP '2026-03-16 11:45:00');
INSERT INTO Prompt (promptID, UsrID, wsID, name, text, category, created_at) VALUES (413, 13, 113, 'Sales Script', 'Create a short discovery call script.', 'Sales', TIMESTAMP '2026-03-16 12:00:00');
INSERT INTO Prompt (promptID, UsrID, wsID, name, text, category, created_at) VALUES (414, 14, 114, 'Translation Check', 'Check this translation for natural wording.', 'Translation', TIMESTAMP '2026-03-16 12:15:00');
INSERT INTO Prompt (promptID, UsrID, wsID, name, text, category, created_at) VALUES (415, 15, 115, 'Resume Review', 'Improve this resume bullet.', 'Career', TIMESTAMP '2026-03-16 12:30:00');

-- -------------------------
-- Persona: 15 rows
-- -------------------------
INSERT INTO Persona (personaID, UsrID, name, instructions, status, created_at) VALUES (501, 1, 'Friendly Tutor', 'Be patient, clear, and practical.', 'active', TIMESTAMP '2026-03-17 09:00:00');
INSERT INTO Persona (personaID, UsrID, name, instructions, status, created_at) VALUES (502, 2, 'Senior Engineer', 'Focus on correctness and maintainability.', 'active', TIMESTAMP '2026-03-17 09:15:00');
INSERT INTO Persona (personaID, UsrID, name, instructions, status, created_at) VALUES (503, 3, 'Spanish Editor', 'Use natural Spanish and concise phrasing.', 'active', TIMESTAMP '2026-03-17 09:30:00');
INSERT INTO Persona (personaID, UsrID, name, instructions, status, created_at) VALUES (504, 4, 'Database TA', 'Explain joins, constraints, and query flow.', 'active', TIMESTAMP '2026-03-17 09:45:00');
INSERT INTO Persona (personaID, UsrID, name, instructions, status, created_at) VALUES (505, 5, 'Business Writer', 'Write polished but direct messages.', 'inactive', TIMESTAMP '2026-03-17 10:00:00');
INSERT INTO Persona (personaID, UsrID, name, instructions, status, created_at) VALUES (506, 6, 'Project Manager', 'Organize information into next steps.', 'active', TIMESTAMP '2026-03-17 10:15:00');
INSERT INTO Persona (personaID, UsrID, name, instructions, status, created_at) VALUES (507, 7, 'Growth Strategist', 'Think in terms of campaigns and funnels.', 'active', TIMESTAMP '2026-03-17 10:30:00');
INSERT INTO Persona (personaID, UsrID, name, instructions, status, created_at) VALUES (508, 8, 'Risk Reviewer', 'Flag legal and compliance risks.', 'active', TIMESTAMP '2026-03-17 10:45:00');
INSERT INTO Persona (personaID, UsrID, name, instructions, status, created_at) VALUES (509, 9, 'Math Coach', 'Show the logic before the final answer.', 'archived', TIMESTAMP '2026-03-17 11:00:00');
INSERT INTO Persona (personaID, UsrID, name, instructions, status, created_at) VALUES (510, 10, 'Research Analyst', 'Separate facts, assumptions, and conclusions.', 'active', TIMESTAMP '2026-03-17 11:15:00');
INSERT INTO Persona (personaID, UsrID, name, instructions, status, created_at) VALUES (511, 11, 'Creative Director', 'Suggest memorable but practical options.', 'active', TIMESTAMP '2026-03-17 11:30:00');
INSERT INTO Persona (personaID, UsrID, name, instructions, status, created_at) VALUES (512, 12, 'Backend Architect', 'Prioritize simple reliable backend design.', 'active', TIMESTAMP '2026-03-17 11:45:00');
INSERT INTO Persona (personaID, UsrID, name, instructions, status, created_at) VALUES (513, 13, 'Sales Coach', 'Make sales language short and non-pushy.', 'active', TIMESTAMP '2026-03-17 12:00:00');
INSERT INTO Persona (personaID, UsrID, name, instructions, status, created_at) VALUES (514, 14, 'Translator', 'Preserve meaning and terminology.', 'active', TIMESTAMP '2026-03-17 12:15:00');
INSERT INTO Persona (personaID, UsrID, name, instructions, status, created_at) VALUES (515, 15, 'Career Coach', 'Make career advice specific and useful.', 'deleted', TIMESTAMP '2026-03-17 12:30:00');

-- -------------------------
-- Conversation: 15 rows
-- -------------------------
INSERT INTO Conversation (CID, UsrID, personaID, wsID, title, created_at) VALUES (601, 1, 501, 101, 'Discrete math help', TIMESTAMP '2026-03-18 09:00:00');
INSERT INTO Conversation (CID, UsrID, personaID, wsID, title, created_at) VALUES (602, 2, 502, 102, 'Java connection debugging', TIMESTAMP '2026-03-18 09:15:00');
INSERT INTO Conversation (CID, UsrID, personaID, wsID, title, created_at) VALUES (603, 3, 503, 103, 'Spanish landing page copy', TIMESTAMP '2026-03-18 09:30:00');
INSERT INTO Conversation (CID, UsrID, personaID, wsID, title, created_at) VALUES (604, 4, 504, 104, 'SQL joins practice', TIMESTAMP '2026-03-18 09:45:00');
INSERT INTO Conversation (CID, UsrID, personaID, wsID, title, created_at) VALUES (605, 5, 505, 105, 'Client email draft', TIMESTAMP '2026-03-18 10:00:00');
INSERT INTO Conversation (CID, UsrID, personaID, wsID, title, created_at) VALUES (606, 6, 506, 106, 'Sprint planning notes', TIMESTAMP '2026-03-18 10:15:00');
INSERT INTO Conversation (CID, UsrID, personaID, wsID, title, created_at) VALUES (607, 7, 507, 107, 'Meta ads strategy', TIMESTAMP '2026-03-18 10:30:00');
INSERT INTO Conversation (CID, UsrID, personaID, wsID, title, created_at) VALUES (608, 8, 508, 108, 'Terms of service review', TIMESTAMP '2026-03-18 10:45:00');
INSERT INTO Conversation (CID, UsrID, personaID, wsID, title, created_at) VALUES (609, 9, 509, 109, 'Calculus practice', TIMESTAMP '2026-03-18 11:00:00');
INSERT INTO Conversation (CID, UsrID, personaID, wsID, title, created_at) VALUES (610, 10, 510, 110, 'Paper summary', TIMESTAMP '2026-03-18 11:15:00');
INSERT INTO Conversation (CID, UsrID, personaID, wsID, title, created_at) VALUES (611, 11, 511, 111, 'Brand name brainstorm', TIMESTAMP '2026-03-18 11:30:00');
INSERT INTO Conversation (CID, UsrID, personaID, wsID, title, created_at) VALUES (612, 12, 512, 112, 'FastAPI route design', TIMESTAMP '2026-03-18 11:45:00');
INSERT INTO Conversation (CID, UsrID, personaID, wsID, title, created_at) VALUES (613, 13, 513, 113, 'Discovery call script', TIMESTAMP '2026-03-18 12:00:00');
INSERT INTO Conversation (CID, UsrID, personaID, wsID, title, created_at) VALUES (614, 14, 514, 114, 'Document translation review', TIMESTAMP '2026-03-18 12:15:00');
INSERT INTO Conversation (CID, UsrID, personaID, wsID, title, created_at) VALUES (615, 15, 515, 115, 'Resume bullet improvement', TIMESTAMP '2026-03-18 12:30:00');

-- -------------------------
-- Message: 30 rows, at least 15 required
-- -------------------------
INSERT INTO Message (MsgID, CID, sender, content, sent_at) VALUES (701, 601, 'user', 'Can you explain induction?', TIMESTAMP '2026-03-18 09:01:00');
INSERT INTO Message (MsgID, CID, sender, content, sent_at) VALUES (702, 601, 'normal', 'Induction proves a statement for all natural numbers.', TIMESTAMP '2026-03-18 09:02:00');
INSERT INTO Message (MsgID, CID, sender, content, sent_at) VALUES (703, 602, 'user', 'My JDBC connection fails.', TIMESTAMP '2026-03-18 09:16:00');
INSERT INTO Message (MsgID, CID, sender, content, sent_at) VALUES (704, 602, 'pro', 'Check the driver, URL, username, and password first.', TIMESTAMP '2026-03-18 09:17:00');
INSERT INTO Message (MsgID, CID, sender, content, sent_at) VALUES (705, 603, 'user', 'Rewrite this copy in Spanish.', TIMESTAMP '2026-03-18 09:31:00');
INSERT INTO Message (MsgID, CID, sender, content, sent_at) VALUES (706, 603, 'normal', 'Claro, aqui tienes una version mas natural.', TIMESTAMP '2026-03-18 09:32:00');
INSERT INTO Message (MsgID, CID, sender, content, sent_at) VALUES (707, 604, 'user', 'How do joins work?', TIMESTAMP '2026-03-18 09:46:00');
INSERT INTO Message (MsgID, CID, sender, content, sent_at) VALUES (708, 604, 'pro', 'A join combines rows based on a matching condition.', TIMESTAMP '2026-03-18 09:47:00');
INSERT INTO Message (MsgID, CID, sender, content, sent_at) VALUES (709, 605, 'user', 'Draft a follow-up email.', TIMESTAMP '2026-03-18 10:01:00');
INSERT INTO Message (MsgID, CID, sender, content, sent_at) VALUES (710, 605, 'normal', 'Here is a concise follow-up email draft.', TIMESTAMP '2026-03-18 10:02:00');
INSERT INTO Message (MsgID, CID, sender, content, sent_at) VALUES (711, 606, 'user', 'Summarize our sprint goals.', TIMESTAMP '2026-03-18 10:16:00');
INSERT INTO Message (MsgID, CID, sender, content, sent_at) VALUES (712, 606, 'pro', 'The main goals are delivery, testing, and launch readiness.', TIMESTAMP '2026-03-18 10:17:00');
INSERT INTO Message (MsgID, CID, sender, content, sent_at) VALUES (713, 607, 'user', 'Give me ad angles.', TIMESTAMP '2026-03-18 10:31:00');
INSERT INTO Message (MsgID, CID, sender, content, sent_at) VALUES (714, 607, 'pro', 'Lead with speed-to-lead and missed follow-up gaps.', TIMESTAMP '2026-03-18 10:32:00');
INSERT INTO Message (MsgID, CID, sender, content, sent_at) VALUES (715, 608, 'user', 'Find risks in this contract.', TIMESTAMP '2026-03-18 10:46:00');
INSERT INTO Message (MsgID, CID, sender, content, sent_at) VALUES (716, 608, 'pro', 'The largest risks are vague liability and termination terms.', TIMESTAMP '2026-03-18 10:47:00');
INSERT INTO Message (MsgID, CID, sender, content, sent_at) VALUES (717, 609, 'user', 'Solve this derivative.', TIMESTAMP '2026-03-18 11:01:00');
INSERT INTO Message (MsgID, CID, sender, content, sent_at) VALUES (718, 609, 'normal', 'Use the power rule and simplify.', TIMESTAMP '2026-03-18 11:02:00');
INSERT INTO Message (MsgID, CID, sender, content, sent_at) VALUES (719, 610, 'user', 'Summarize this paper.', TIMESTAMP '2026-03-18 11:16:00');
INSERT INTO Message (MsgID, CID, sender, content, sent_at) VALUES (720, 610, 'pro', 'The paper studies model behavior under zero RL training.', TIMESTAMP '2026-03-18 11:17:00');
INSERT INTO Message (MsgID, CID, sender, content, sent_at) VALUES (721, 611, 'user', 'Suggest a tech company name.', TIMESTAMP '2026-03-18 11:31:00');
INSERT INTO Message (MsgID, CID, sender, content, sent_at) VALUES (722, 611, 'normal', 'A strong name should be short, searchable, and memorable.', TIMESTAMP '2026-03-18 11:32:00');
INSERT INTO Message (MsgID, CID, sender, content, sent_at) VALUES (723, 612, 'user', 'Design auth routes.', TIMESTAMP '2026-03-18 11:46:00');
INSERT INTO Message (MsgID, CID, sender, content, sent_at) VALUES (724, 612, 'pro', 'Use login, logout, refresh, and current-user routes.', TIMESTAMP '2026-03-18 11:47:00');
INSERT INTO Message (MsgID, CID, sender, content, sent_at) VALUES (725, 613, 'user', 'Make this sales script less pushy.', TIMESTAMP '2026-03-18 12:01:00');
INSERT INTO Message (MsgID, CID, sender, content, sent_at) VALUES (726, 613, 'normal', 'Ask about the problem before pitching the solution.', TIMESTAMP '2026-03-18 12:02:00');
INSERT INTO Message (MsgID, CID, sender, content, sent_at) VALUES (727, 614, 'user', 'Check terminology consistency.', TIMESTAMP '2026-03-18 12:16:00');
INSERT INTO Message (MsgID, CID, sender, content, sent_at) VALUES (728, 614, 'pro', 'The terminology is mostly consistent except for two repeated terms.', TIMESTAMP '2026-03-18 12:17:00');
INSERT INTO Message (MsgID, CID, sender, content, sent_at) VALUES (729, 615, 'user', 'Improve this resume bullet.', TIMESTAMP '2026-03-18 12:31:00');
INSERT INTO Message (MsgID, CID, sender, content, sent_at) VALUES (730, 615, 'normal', 'Start with an action verb and add measurable impact.', TIMESTAMP '2026-03-18 12:32:00');

-- -------------------------
-- MsgRating: 15 rows
-- -------------------------
INSERT INTO MsgRating (MsgID, rating, usr_feedback, rated_at) VALUES (702, 1, 'Clear explanation.', TIMESTAMP '2026-03-18 09:05:00');
INSERT INTO MsgRating (MsgID, rating, usr_feedback, rated_at) VALUES (704, 1, 'Helpful debugging order.', TIMESTAMP '2026-03-18 09:20:00');
INSERT INTO MsgRating (MsgID, rating, usr_feedback, rated_at) VALUES (706, -1, 'Needed more detail.', TIMESTAMP '2026-03-18 09:35:00');
INSERT INTO MsgRating (MsgID, rating, usr_feedback, rated_at) VALUES (708, 1, 'Good join definition.', TIMESTAMP '2026-03-18 09:50:00');
INSERT INTO MsgRating (MsgID, rating, usr_feedback, rated_at) VALUES (710, 1, 'Useful draft.', TIMESTAMP '2026-03-18 10:05:00');
INSERT INTO MsgRating (MsgID, rating, usr_feedback, rated_at) VALUES (712, 1, 'Actionable summary.', TIMESTAMP '2026-03-18 10:20:00');
INSERT INTO MsgRating (MsgID, rating, usr_feedback, rated_at) VALUES (714, 1, 'Strong marketing angle.', TIMESTAMP '2026-03-18 10:35:00');
INSERT INTO MsgRating (MsgID, rating, usr_feedback, rated_at) VALUES (716, -1, 'Too broad.', TIMESTAMP '2026-03-18 10:50:00');
INSERT INTO MsgRating (MsgID, rating, usr_feedback, rated_at) VALUES (718, 1, 'Good reminder.', TIMESTAMP '2026-03-18 11:05:00');
INSERT INTO MsgRating (MsgID, rating, usr_feedback, rated_at) VALUES (720, 1, 'Accurate summary.', TIMESTAMP '2026-03-18 11:20:00');
INSERT INTO MsgRating (MsgID, rating, usr_feedback, rated_at) VALUES (722, 1, 'Good naming criteria.', TIMESTAMP '2026-03-18 11:35:00');
INSERT INTO MsgRating (MsgID, rating, usr_feedback, rated_at) VALUES (724, 1, 'Clean route set.', TIMESTAMP '2026-03-18 11:50:00');
INSERT INTO MsgRating (MsgID, rating, usr_feedback, rated_at) VALUES (726, -1, 'Wanted a full script.', TIMESTAMP '2026-03-18 12:05:00');
INSERT INTO MsgRating (MsgID, rating, usr_feedback, rated_at) VALUES (728, 1, 'Found the issue.', TIMESTAMP '2026-03-18 12:20:00');
INSERT INTO MsgRating (MsgID, rating, usr_feedback, rated_at) VALUES (730, 1, 'Better resume language.', TIMESTAMP '2026-03-18 12:35:00');

-- -------------------------
-- Bookmark: 15 rows
-- These obey access trigger: each user bookmarks a message in their own conversation.
-- -------------------------
INSERT INTO Bookmark (MsgID, UsrID, bookmarked_at) VALUES (702, 1, TIMESTAMP '2026-03-19 09:00:00');
INSERT INTO Bookmark (MsgID, UsrID, bookmarked_at) VALUES (704, 2, TIMESTAMP '2026-03-19 09:15:00');
INSERT INTO Bookmark (MsgID, UsrID, bookmarked_at) VALUES (706, 3, TIMESTAMP '2026-03-19 09:30:00');
INSERT INTO Bookmark (MsgID, UsrID, bookmarked_at) VALUES (708, 4, TIMESTAMP '2026-03-19 09:45:00');
INSERT INTO Bookmark (MsgID, UsrID, bookmarked_at) VALUES (710, 5, TIMESTAMP '2026-03-19 10:00:00');
INSERT INTO Bookmark (MsgID, UsrID, bookmarked_at) VALUES (712, 6, TIMESTAMP '2026-03-19 10:15:00');
INSERT INTO Bookmark (MsgID, UsrID, bookmarked_at) VALUES (714, 7, TIMESTAMP '2026-03-19 10:30:00');
INSERT INTO Bookmark (MsgID, UsrID, bookmarked_at) VALUES (716, 8, TIMESTAMP '2026-03-19 10:45:00');
INSERT INTO Bookmark (MsgID, UsrID, bookmarked_at) VALUES (718, 9, TIMESTAMP '2026-03-19 11:00:00');
INSERT INTO Bookmark (MsgID, UsrID, bookmarked_at) VALUES (720, 10, TIMESTAMP '2026-03-19 11:15:00');
INSERT INTO Bookmark (MsgID, UsrID, bookmarked_at) VALUES (722, 11, TIMESTAMP '2026-03-19 11:30:00');
INSERT INTO Bookmark (MsgID, UsrID, bookmarked_at) VALUES (724, 12, TIMESTAMP '2026-03-19 11:45:00');
INSERT INTO Bookmark (MsgID, UsrID, bookmarked_at) VALUES (726, 13, TIMESTAMP '2026-03-19 12:00:00');
INSERT INTO Bookmark (MsgID, UsrID, bookmarked_at) VALUES (728, 14, TIMESTAMP '2026-03-19 12:15:00');
INSERT INTO Bookmark (MsgID, UsrID, bookmarked_at) VALUES (730, 15, TIMESTAMP '2026-03-19 12:30:00');

-- -------------------------
-- SupportAgent: 15 rows
-- -------------------------
INSERT INTO SupportAgent (sno, name, status, hired_at) VALUES (801, 'Nora Adams', 'active', TIMESTAMP '2025-01-01 09:00:00');
INSERT INTO SupportAgent (sno, name, status, hired_at) VALUES (802, 'Owen Baker', 'active', TIMESTAMP '2025-01-02 09:00:00');
INSERT INTO SupportAgent (sno, name, status, hired_at) VALUES (803, 'Priya Chen', 'active', TIMESTAMP '2025-01-03 09:00:00');
INSERT INTO SupportAgent (sno, name, status, hired_at) VALUES (804, 'Mateo Diaz', 'inactive', TIMESTAMP '2025-01-04 09:00:00');
INSERT INTO SupportAgent (sno, name, status, hired_at) VALUES (805, 'Ruby Evans', 'active', TIMESTAMP '2025-01-05 09:00:00');
INSERT INTO SupportAgent (sno, name, status, hired_at) VALUES (806, 'Leo Foster', 'active', TIMESTAMP '2025-01-06 09:00:00');
INSERT INTO SupportAgent (sno, name, status, hired_at) VALUES (807, 'Grace Hall', 'terminated', TIMESTAMP '2025-01-07 09:00:00');
INSERT INTO SupportAgent (sno, name, status, hired_at) VALUES (808, 'Henry King', 'active', TIMESTAMP '2025-01-08 09:00:00');
INSERT INTO SupportAgent (sno, name, status, hired_at) VALUES (809, 'Iris Lopez', 'active', TIMESTAMP '2025-01-09 09:00:00');
INSERT INTO SupportAgent (sno, name, status, hired_at) VALUES (810, 'Jack Miller', 'inactive', TIMESTAMP '2025-01-10 09:00:00');
INSERT INTO SupportAgent (sno, name, status, hired_at) VALUES (811, 'Kira Nelson', 'active', TIMESTAMP '2025-01-11 09:00:00');
INSERT INTO SupportAgent (sno, name, status, hired_at) VALUES (812, 'Mason Ortiz', 'active', TIMESTAMP '2025-01-12 09:00:00');
INSERT INTO SupportAgent (sno, name, status, hired_at) VALUES (813, 'Lena Perez', 'active', TIMESTAMP '2025-01-13 09:00:00');
INSERT INTO SupportAgent (sno, name, status, hired_at) VALUES (814, 'Eli Quinn', 'active', TIMESTAMP '2025-01-14 09:00:00');
INSERT INTO SupportAgent (sno, name, status, hired_at) VALUES (815, 'Zoe Reed', 'active', TIMESTAMP '2025-01-15 09:00:00');

-- -------------------------
-- SupportTicket: 15 rows
-- closed tickets must have closed_at.
-- -------------------------
INSERT INTO SupportTicket (ticketID, UsrID, sno, topic, status, created_at, closed_at) VALUES (901, 1, 801, 'Cannot reset password', 'open', TIMESTAMP '2026-03-20 09:00:00', NULL);
INSERT INTO SupportTicket (ticketID, UsrID, sno, topic, status, created_at, closed_at) VALUES (902, 2, 802, 'Billing question', 'pending', TIMESTAMP '2026-03-20 09:15:00', NULL);
INSERT INTO SupportTicket (ticketID, UsrID, sno, topic, status, created_at, closed_at) VALUES (903, 3, 803, 'Upgrade failed', 'closed', TIMESTAMP '2026-03-20 09:30:00', TIMESTAMP '2026-03-21 09:30:00');
INSERT INTO SupportTicket (ticketID, UsrID, sno, topic, status, created_at, closed_at) VALUES (904, 4, 804, 'Model output issue', 'open', TIMESTAMP '2026-03-20 09:45:00', NULL);
INSERT INTO SupportTicket (ticketID, UsrID, sno, topic, status, created_at, closed_at) VALUES (905, 5, 805, 'Invoice refund request', 'closed', TIMESTAMP '2026-03-20 10:00:00', TIMESTAMP '2026-03-22 10:00:00');
INSERT INTO SupportTicket (ticketID, UsrID, sno, topic, status, created_at, closed_at) VALUES (906, 6, 806, 'Workspace invite problem', 'pending', TIMESTAMP '2026-03-20 10:15:00', NULL);
INSERT INTO SupportTicket (ticketID, UsrID, sno, topic, status, created_at, closed_at) VALUES (907, 7, 807, 'Prompt library missing item', 'open', TIMESTAMP '2026-03-20 10:30:00', NULL);
INSERT INTO SupportTicket (ticketID, UsrID, sno, topic, status, created_at, closed_at) VALUES (908, 8, 808, 'Enterprise security question', 'closed', TIMESTAMP '2026-03-20 10:45:00', TIMESTAMP '2026-03-22 11:45:00');
INSERT INTO SupportTicket (ticketID, UsrID, sno, topic, status, created_at, closed_at) VALUES (909, 9, 809, 'Student discount verification', 'pending', TIMESTAMP '2026-03-20 11:00:00', NULL);
INSERT INTO SupportTicket (ticketID, UsrID, sno, topic, status, created_at, closed_at) VALUES (910, 10, 810, 'Research export bug', 'open', TIMESTAMP '2026-03-20 11:15:00', NULL);
INSERT INTO SupportTicket (ticketID, UsrID, sno, topic, status, created_at, closed_at) VALUES (911, 11, 811, 'Creator plan media issue', 'closed', TIMESTAMP '2026-03-20 11:30:00', TIMESTAMP '2026-03-21 12:30:00');
INSERT INTO SupportTicket (ticketID, UsrID, sno, topic, status, created_at, closed_at) VALUES (912, 12, 812, 'API key setup', 'pending', TIMESTAMP '2026-03-20 11:45:00', NULL);
INSERT INTO SupportTicket (ticketID, UsrID, sno, topic, status, created_at, closed_at) VALUES (913, 13, 813, 'Agency seat limit', 'open', TIMESTAMP '2026-03-20 12:00:00', NULL);
INSERT INTO SupportTicket (ticketID, UsrID, sno, topic, status, created_at, closed_at) VALUES (914, 14, 814, 'Education workspace setup', 'closed', TIMESTAMP '2026-03-20 12:15:00', TIMESTAMP '2026-03-23 12:15:00');
INSERT INTO SupportTicket (ticketID, UsrID, sno, topic, status, created_at, closed_at) VALUES (915, 15, 815, 'Trial extension request', 'open', TIMESTAMP '2026-03-20 12:30:00', NULL);

COMMIT;

-- Quick sanity checks:
-- SELECT COUNT(*) FROM Plan;
-- SELECT COUNT(*) FROM "User";
-- SELECT COUNT(*) FROM Workspace;
-- SELECT COUNT(*) FROM WorkspaceMember;
-- SELECT COUNT(*) FROM PaymentMethod;
-- SELECT COUNT(*) FROM Invoice;
-- SELECT COUNT(*) FROM Prompt;
-- SELECT COUNT(*) FROM Persona;
-- SELECT COUNT(*) FROM Conversation;
-- SELECT COUNT(*) FROM Message;
-- SELECT COUNT(*) FROM MsgRating;
-- SELECT COUNT(*) FROM Bookmark;
-- SELECT COUNT(*) FROM SupportAgent;
-- SELECT COUNT(*) FROM SupportTicket;
