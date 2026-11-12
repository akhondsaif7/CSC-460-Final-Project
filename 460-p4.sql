-- =========================================================
-- TABLES
-- =========================================================
CREATE TABLE Plan (
    plan            VARCHAR2(50) PRIMARY KEY,
    max_daily_msg   NUMBER(10) NOT NULL,
    pro_model       NUMBER(1) DEFAULT 0 NOT NULL,
    cost            int NOT NULL,
    CONSTRAINT non_neg_cost
        CHECK (cost >= 0),
    CONSTRAINT chk_plan_max_daily_msg
        CHECK (max_daily_msg >= 0),
    CONSTRAINT chk_plan_pro_model
        CHECK (pro_model IN (0, 1))
);
CREATE TABLE "User" (
    UsrID               NUMBER(10) PRIMARY KEY,
    full_name           VARCHAR2(100) NOT NULL,
    email               VARCHAR2(255) NOT NULL,
    created_at          TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    preferred_language  VARCHAR2(30),
    plan                VARCHAR2(50) NOT NULL,
    CONSTRAINT uq_user_email
        UNIQUE (email),
    CONSTRAINT fk_user_plan
        FOREIGN KEY (plan)
        REFERENCES Plan(plan),
    CONSTRAINT chk_user_email_not_blank
        CHECK (TRIM(email) IS NOT NULL),
    CONSTRAINT chk_user_full_name_not_blank
        CHECK (TRIM(full_name) IS NOT NULL)
);
CREATE TABLE Workspace (
    wsID                NUMBER(10) PRIMARY KEY,
    owner               NUMBER(10) NOT NULL,
    created_at          TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    preferred_language  VARCHAR2(30),
    plan                VARCHAR2(50) NOT NULL,
    CONSTRAINT fk_workspace_owner
        FOREIGN KEY (owner)
        REFERENCES "User"(UsrID),
    CONSTRAINT fk_workspace_plan
        FOREIGN KEY (plan)
        REFERENCES Plan(plan)
);
CREATE TABLE WorkspaceMember (
    wsID        NUMBER(10) NOT NULL,
    UsrID       NUMBER(10) NOT NULL,
    role        VARCHAR2(20) NOT NULL,
    joined_at   TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_workspace_member
        PRIMARY KEY (wsID, UsrID),
    CONSTRAINT fk_workspace_member_workspace
        FOREIGN KEY (wsID)
        REFERENCES Workspace(wsID)
        ON DELETE CASCADE,
    CONSTRAINT fk_workspace_member_user
        FOREIGN KEY (UsrID)
        REFERENCES "User"(UsrID)
        ON DELETE CASCADE,
    CONSTRAINT chk_workspace_member_role
        CHECK (role IN ('owner', 'admin', 'member', 'viewer'))
);
CREATE TABLE PaymentMethod (
    payment_method_ID   NUMBER(10) PRIMARY KEY,
    UsrID               NUMBER(10) NOT NULL,
    type                VARCHAR2(50) NOT NULL,
    billing_addr        VARCHAR2(255) NOT NULL,
    status              VARCHAR2(20) DEFAULT 'active' NOT NULL,
    -- Needed so Invoice can prove that the selected payment method belongs to the invoice user.
    CONSTRAINT uq_pm_user_pair
        UNIQUE (payment_method_ID, UsrID),
    CONSTRAINT fk_pm_user
        FOREIGN KEY (UsrID)
        REFERENCES "User"(UsrID),
    CONSTRAINT chk_pm_type_not_blank
        CHECK (TRIM(type) IS NOT NULL),
    CONSTRAINT chk_pm_billing_not_blank
        CHECK (TRIM(billing_addr) IS NOT NULL),
    CONSTRAINT chk_pm_status
        CHECK (status IN ('active', 'inactive', 'expired', 'archived', 'deleted'))
);
CREATE TABLE Invoice (
    InvNo               NUMBER(10) PRIMARY KEY,
    UsrID               NUMBER(10) NOT NULL,
    amount              NUMBER(12, 2) NOT NULL,
    invoice_date        DATE DEFAULT TRUNC(SYSDATE) NOT NULL,
    payment_method_ID   NUMBER(10) NOT NULL,
    status              VARCHAR2(20) DEFAULT 'open' NOT NULL,
    CONSTRAINT fk_invoice_user
        FOREIGN KEY (UsrID)
        REFERENCES "User"(UsrID),
    -- Assertion: an invoice can only use a payment method owned by that same user.
    CONSTRAINT fk_inv_pm_same_user
        FOREIGN KEY (payment_method_ID, UsrID)
        REFERENCES PaymentMethod(payment_method_ID, UsrID),
    CONSTRAINT chk_invoice_amount
        CHECK (amount >= 0),
    CONSTRAINT chk_invoice_status
        CHECK (status IN ('draft', 'open', 'paid', 'failed', 'void', 'refunded', 'overdue'))
);
CREATE TABLE Prompt (
    promptID    NUMBER(10) PRIMARY KEY,
    UsrID       NUMBER(10) NOT NULL,
    wsID        NUMBER(10),
    name        VARCHAR2(150) NOT NULL,
    text        CLOB NOT NULL,
    category    VARCHAR2(80),
    created_at  TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT fk_prompt_user
        FOREIGN KEY (UsrID)
        REFERENCES "User"(UsrID)
        ON DELETE CASCADE,
    CONSTRAINT fk_prompt_workspace
        FOREIGN KEY (wsID)
        REFERENCES Workspace(wsID)
        ON DELETE SET NULL,
    -- Assertion: if Prompt.wsID is not NULL, the prompt owner must be a member of that workspace.
    CONSTRAINT fk_prompt_user_is_wm
        FOREIGN KEY (wsID, UsrID)
        REFERENCES WorkspaceMember(wsID, UsrID),
    CONSTRAINT chk_prompt_name_not_blank
        CHECK (TRIM(name) IS NOT NULL)
);
CREATE TABLE Persona (
    personaID       NUMBER(10) PRIMARY KEY,
    UsrID           NUMBER(10) NOT NULL,
    name            VARCHAR2(150) NOT NULL,
    instructions    CLOB NOT NULL,
    status          VARCHAR2(20) DEFAULT 'active' NOT NULL,
    created_at      TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    -- Needed so Conversation can prove that the selected persona belongs to the conversation user.
    CONSTRAINT uq_persona_user_pair
        UNIQUE (personaID, UsrID),
    CONSTRAINT fk_persona_user
        FOREIGN KEY (UsrID)
        REFERENCES "User"(UsrID)
        ON DELETE CASCADE,
    CONSTRAINT chk_persona_name_not_blank
        CHECK (TRIM(name) IS NOT NULL),
    CONSTRAINT chk_persona_status
        CHECK (status IN ('active', 'inactive', 'archived', 'deleted'))
);
CREATE TABLE Conversation (
    CID         NUMBER(10) PRIMARY KEY,
    UsrID       NUMBER(10) NOT NULL,
    personaID   NUMBER(10),
    wsID        NUMBER(10),
    title       VARCHAR2(200),
    created_at  TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT fk_conversation_user
        FOREIGN KEY (UsrID)
        REFERENCES "User"(UsrID)
        ON DELETE CASCADE,
    CONSTRAINT fk_conversation_workspace
        FOREIGN KEY (wsID)
        REFERENCES Workspace(wsID)
        ON DELETE SET NULL,
    -- Assertion: if Conversation.personaID is not NULL, the persona must belong to the same user.
    CONSTRAINT fk_conv_persona_same_user
        FOREIGN KEY (personaID, UsrID)
        REFERENCES Persona(personaID, UsrID),
    -- Assertion: if Conversation.wsID is not NULL, the conversation user must be a member of that workspace.
    CONSTRAINT fk_conv_user_is_wm
        FOREIGN KEY (wsID, UsrID)
        REFERENCES WorkspaceMember(wsID, UsrID)
);
CREATE TABLE Message (
    MsgID       NUMBER(10) PRIMARY KEY,
    CID         NUMBER(10) NOT NULL,
    sender      VARCHAR2(100) NOT NULL,
    content     CLOB NOT NULL,
    sent_at     TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT fk_message_conversation
        FOREIGN KEY (CID)
        REFERENCES Conversation(CID)
        ON DELETE CASCADE,
    CONSTRAINT chk_message_sender
        CHECK (sender IN ('user', 'normal', 'pro'))
);
CREATE TABLE MsgRating (
    MsgID           NUMBER(10) PRIMARY KEY,
    rating          NUMBER(1),
    usr_feedback    CLOB,
    rated_at        TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT fk_msg_rating_message
        FOREIGN KEY (MsgID)
        REFERENCES Message(MsgID)
        ON DELETE CASCADE,
    CONSTRAINT chk_msg_rating_value
        CHECK (rating IN (0, 1))
);
CREATE TABLE Bookmark (
    MsgID           NUMBER(10) NOT NULL,
    UsrID           NUMBER(10) NOT NULL,
    bookmarked_at   TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_bookmark
        PRIMARY KEY (MsgID, UsrID),
    CONSTRAINT fk_bookmark_message
        FOREIGN KEY (MsgID)
        REFERENCES Message(MsgID)
        ON DELETE CASCADE,
    CONSTRAINT fk_bookmark_user
        FOREIGN KEY (UsrID)
        REFERENCES "User"(UsrID)
        ON DELETE CASCADE
);
CREATE TABLE SupportAgent (
    sno        NUMBER(10) PRIMARY KEY,
    name       VARCHAR2(100) NOT NULL,
    status     VARCHAR2(20) DEFAULT 'active' NOT NULL,
    hired_at   TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT chk_sa_name_not_blank
        CHECK (TRIM(name) IS NOT NULL),
    CONSTRAINT chk_sa_status
        CHECK (status IN ('active', 'inactive', 'terminated'))
);
CREATE TABLE SupportTicket (
    ticketID    NUMBER(10) PRIMARY KEY,
    UsrID       NUMBER(10) NOT NULL,
    sno         NUMBER(10) NOT NULL,
    topic       VARCHAR2(200) NOT NULL,
    status      VARCHAR2(20) DEFAULT 'open' NOT NULL,
    created_at  TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    closed_at   TIMESTAMP,
    CONSTRAINT fk_support_ticket_user
        FOREIGN KEY (UsrID)
        REFERENCES "User"(UsrID)
        ON DELETE CASCADE,
    CONSTRAINT fk_support_ticket_agent
        FOREIGN KEY (sno)
        REFERENCES SupportAgent(sno),
    CONSTRAINT chk_st_topic_not_blank
        CHECK (TRIM(topic) IS NOT NULL),
    CONSTRAINT chk_st_status
        CHECK (status IN ('open', 'pending', 'closed')),
    CONSTRAINT chk_st_closed_at
        CHECK (closed_at IS NULL OR closed_at >= created_at),
    CONSTRAINT chk_st_closed_status
        CHECK (status <> 'closed' OR closed_at IS NOT NULL)
);
-- =========================================================
-- INDEXES
-- =========================================================
CREATE INDEX idx_user_plan ON "User"(plan);
CREATE INDEX idx_workspace_owner ON Workspace(owner);
CREATE INDEX idx_workspace_plan ON Workspace(plan);
CREATE INDEX idx_wm_user ON WorkspaceMember(UsrID);
CREATE INDEX idx_pm_user ON PaymentMethod(UsrID);
CREATE INDEX idx_invoice_user ON Invoice(UsrID);
CREATE INDEX idx_inv_pm ON Invoice(payment_method_ID);
CREATE INDEX idx_prompt_user ON Prompt(UsrID);
CREATE INDEX idx_prompt_workspace ON Prompt(wsID);
CREATE INDEX idx_persona_user ON Persona(UsrID);
CREATE INDEX idx_conversation_user ON Conversation(UsrID);
CREATE INDEX idx_conv_persona ON Conversation(personaID);
CREATE INDEX idx_conv_workspace ON Conversation(wsID);
CREATE INDEX idx_msg_conversation ON Message(CID);
CREATE INDEX idx_bookmark_user ON Bookmark(UsrID);
CREATE INDEX idx_st_user ON SupportTicket(UsrID);
CREATE INDEX idx_st_agent ON SupportTicket(sno);
