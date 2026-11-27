-- ============================================================================
-- CLOTHING SHOP V2 - REALTIME CHAT, STOCK HISTORY, BANNED WORDS & STAFF SEED
-- Chay file nay 1 lan SAU KHI da chay database\ClothingShopDB.sql
-- Ho tro chay lai nhieu lan (Idempotent) ma khong bi trung du lieu
-- ============================================================================
USE ClothingShopDB;
GO

-- >>> NGUON GOP: database\V2_Chat_And_Realtime.sql <<<
GO

-- 1. Bo sung cot Version cho Optimistic Locking tren Products & ProductVariants
IF COL_LENGTH('Products', 'Version') IS NULL
    ALTER TABLE Products ADD Version BIGINT NOT NULL CONSTRAINT DF_Products_Version DEFAULT 0;
GO

IF COL_LENGTH('ProductVariants', 'Version') IS NULL
    ALTER TABLE ProductVariants ADD Version BIGINT NOT NULL CONSTRAINT DF_ProductVariants_Version DEFAULT 0;
GO

-- 2. Bang cuoc hoi thoai ho tro khach hang & noi bo
IF OBJECT_ID('ChatConversations', 'U') IS NULL
CREATE TABLE ChatConversations (
    ConversationId BIGINT IDENTITY(1,1) PRIMARY KEY,
    CustomerId INT NOT NULL,
    AssignedStaffId INT NULL,
    Title NVARCHAR(150) NOT NULL,
    Status VARCHAR(20) NOT NULL DEFAULT 'OPEN', -- OPEN | CLOSED | INTERNAL
    CreatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    UpdatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT FK_ChatConv_Customer FOREIGN KEY (CustomerId) REFERENCES Users(UserId),
    CONSTRAINT FK_ChatConv_Staff FOREIGN KEY (AssignedStaffId) REFERENCES Users(UserId)
);
GO

-- 3. Bang thanh vien tham gia hoi thoai
IF OBJECT_ID('ChatParticipants', 'U') IS NULL
CREATE TABLE ChatParticipants (
    ParticipantId BIGINT IDENTITY(1,1) PRIMARY KEY,
    ConversationId BIGINT NOT NULL,
    UserId INT NOT NULL,
    RoleInChat VARCHAR(20) NOT NULL, -- CUSTOMER | STAFF | ADMIN
    JoinedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    LastReadMessageId BIGINT NULL,
    CONSTRAINT UQ_ChatPart_Conv_User UNIQUE (ConversationId, UserId),
    CONSTRAINT FK_ChatPart_Conv FOREIGN KEY (ConversationId) REFERENCES ChatConversations(ConversationId) ON DELETE CASCADE,
    CONSTRAINT FK_ChatPart_User FOREIGN KEY (UserId) REFERENCES Users(UserId)
);
GO

-- 4. Bang tin nhan
IF OBJECT_ID('ChatMessages', 'U') IS NULL
CREATE TABLE ChatMessages (
    MessageId BIGINT IDENTITY(1,1) PRIMARY KEY,
    ConversationId BIGINT NOT NULL,
    SenderId INT NOT NULL,
    OriginalContent NVARCHAR(2000) NOT NULL,
    FilteredContent NVARCHAR(2000) NOT NULL,
    IsMasked BIT NOT NULL DEFAULT 0,
    CreatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT FK_ChatMsg_Conv FOREIGN KEY (ConversationId) REFERENCES ChatConversations(ConversationId) ON DELETE CASCADE,
    CONSTRAINT FK_ChatMsg_Sender FOREIGN KEY (SenderId) REFERENCES Users(UserId)
);
GO

-- 5. Bang tu cam (Admin quan ly)
IF OBJECT_ID('BannedWords', 'U') IS NULL
CREATE TABLE BannedWords (
    WordId INT IDENTITY(1,1) PRIMARY KEY,
    Word NVARCHAR(100) NOT NULL,
    NormalizedWord VARCHAR(100) NOT NULL,
    Action VARCHAR(10) NOT NULL DEFAULT 'MASK', -- MASK | BLOCK
    Severity VARCHAR(15) NOT NULL DEFAULT 'MEDIUM',
    IsActive BIT NOT NULL DEFAULT 1,
    CreatedBy INT NULL,
    CreatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT UQ_BannedWords_Norm UNIQUE (NormalizedWord)
);
GO

-- 6. Bang nhat ky vi pham bo loc tu cam
IF OBJECT_ID('ChatViolations', 'U') IS NULL
CREATE TABLE ChatViolations (
    ViolationId BIGINT IDENTITY(1,1) PRIMARY KEY,
    UserId INT NOT NULL,
    ConversationId BIGINT NULL,
    RawContent NVARCHAR(2000) NOT NULL,
    MatchedWord NVARCHAR(100) NOT NULL,
    ActionTaken VARCHAR(10) NOT NULL, -- MASK | BLOCK
    CreatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT FK_ChatViol_User FOREIGN KEY (UserId) REFERENCES Users(UserId)
);
GO

-- Du lieu mau tu cam
IF NOT EXISTS (SELECT 1 FROM BannedWords)
BEGIN
    INSERT INTO BannedWords (Word, NormalizedWord, Action, Severity) VALUES
    (N'lá»«a Ä‘áº£o', 'lua dao', 'BLOCK', 'HIGH'),
    (N'hÃ ng giáº£', 'hang gia', 'BLOCK', 'HIGH'),
    (N'Ä‘á»“ ngá»‘c', 'do ngoc', 'MASK', 'LOW'),
    (N'ngu ngá»‘c', 'ngu ngoc', 'MASK', 'MEDIUM'),
    (N'spam', 'spam', 'MASK', 'LOW');
END
GO

GO

-- >>> NGUON GOP: scripts\create-stock-history-table.sql <<<
IF OBJECT_ID('dbo.StockAdjustmentLogs', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.StockAdjustmentLogs (
        LogId INT IDENTITY(1,1) PRIMARY KEY,
        VariantId INT NOT NULL,
        Sku NVARCHAR(100) NOT NULL,
        PreviousStock INT NOT NULL,
        DeltaStock INT NOT NULL,
        NewStock INT NOT NULL,
        Reason NVARCHAR(500) NULL,
        AdjustedBy NVARCHAR(150) NULL,
        AdjustedAt DATETIME2 NOT NULL DEFAULT GETDATE()
    );
    CREATE INDEX IX_StockAdjustmentLogs_AdjustedAt ON dbo.StockAdjustmentLogs(AdjustedAt DESC);
END

GO

-- >>> NGUON GOP: scripts\fix-banned-words-utf8.sql <<<
UPDATE BannedWords SET Word = N'lừa đảo', NormalizedWord = 'lua dao' WHERE NormalizedWord = 'lua dao';
UPDATE BannedWords SET Word = N'hàng giả', NormalizedWord = 'hang gia' WHERE NormalizedWord = 'hang gia';
UPDATE BannedWords SET Word = N'đồ ngốc', NormalizedWord = 'do ngoc' WHERE NormalizedWord = 'do ngoc';
UPDATE BannedWords SET Word = N'ngu ngốc', NormalizedWord = 'ngu ngoc' WHERE NormalizedWord = 'ngu ngoc';
SELECT BannedWordId, Word, NormalizedWord, Action FROM BannedWords ORDER BY BannedWordId;

GO

-- >>> NGUON GOP: scripts\seed-100-banned-words.sql <<<
-- Nap danh sach tu cam (Tieng Viet + Tieng Anh) vao BannedWords
IF EXISTS (SELECT 1 FROM BannedWords WHERE NormalizedWord = N'do ngu')
    UPDATE BannedWords SET Word = N'đồ ngu', [Action] = 'MASK', Severity = 'MEDIUM', IsActive = 1 WHERE NormalizedWord = N'do ngu';
ELSE
    INSERT INTO BannedWords (Word, NormalizedWord, [Action], Severity, IsActive, CreatedAt) VALUES (N'đồ ngu', N'do ngu', 'MASK', 'MEDIUM', 1, GETDATE());
IF EXISTS (SELECT 1 FROM BannedWords WHERE NormalizedWord = N'ngu ngoc')
    UPDATE BannedWords SET Word = N'ngu ngốc', [Action] = 'MASK', Severity = 'MEDIUM', IsActive = 1 WHERE NormalizedWord = N'ngu ngoc';
ELSE
    INSERT INTO BannedWords (Word, NormalizedWord, [Action], Severity, IsActive, CreatedAt) VALUES (N'ngu ngốc', N'ngu ngoc', 'MASK', 'MEDIUM', 1, GETDATE());
IF EXISTS (SELECT 1 FROM BannedWords WHERE NormalizedWord = N'ngu si')
    UPDATE BannedWords SET Word = N'ngu si', [Action] = 'MASK', Severity = 'MEDIUM', IsActive = 1 WHERE NormalizedWord = N'ngu si';
ELSE
    INSERT INTO BannedWords (Word, NormalizedWord, [Action], Severity, IsActive, CreatedAt) VALUES (N'ngu si', N'ngu si', 'MASK', 'MEDIUM', 1, GETDATE());
IF EXISTS (SELECT 1 FROM BannedWords WHERE NormalizedWord = N'ngu xuan')
    UPDATE BannedWords SET Word = N'ngu xuẩn', [Action] = 'BLOCK', Severity = 'HIGH', IsActive = 1 WHERE NormalizedWord = N'ngu xuan';
ELSE
    INSERT INTO BannedWords (Word, NormalizedWord, [Action], Severity, IsActive, CreatedAt) VALUES (N'ngu xuẩn', N'ngu xuan', 'BLOCK', 'HIGH', 1, GETDATE());
IF EXISTS (SELECT 1 FROM BannedWords WHERE NormalizedWord = N'do ngoc')
    UPDATE BannedWords SET Word = N'đồ ngốc', [Action] = 'MASK', Severity = 'MEDIUM', IsActive = 1 WHERE NormalizedWord = N'do ngoc';
ELSE
    INSERT INTO BannedWords (Word, NormalizedWord, [Action], Severity, IsActive, CreatedAt) VALUES (N'đồ ngốc', N'do ngoc', 'MASK', 'MEDIUM', 1, GETDATE());
IF EXISTS (SELECT 1 FROM BannedWords WHERE NormalizedWord = N'dan don')
    UPDATE BannedWords SET Word = N'đần độn', [Action] = 'MASK', Severity = 'MEDIUM', IsActive = 1 WHERE NormalizedWord = N'dan don';
ELSE
    INSERT INTO BannedWords (Word, NormalizedWord, [Action], Severity, IsActive, CreatedAt) VALUES (N'đần độn', N'dan don', 'MASK', 'MEDIUM', 1, GETDATE());
IF EXISTS (SELECT 1 FROM BannedWords WHERE NormalizedWord = N'oc cho')
    UPDATE BannedWords SET Word = N'óc chó', [Action] = 'BLOCK', Severity = 'HIGH', IsActive = 1 WHERE NormalizedWord = N'oc cho';
ELSE
    INSERT INTO BannedWords (Word, NormalizedWord, [Action], Severity, IsActive, CreatedAt) VALUES (N'óc chó', N'oc cho', 'BLOCK', 'HIGH', 1, GETDATE());
IF EXISTS (SELECT 1 FROM BannedWords WHERE NormalizedWord = N'oc lon')
    UPDATE BannedWords SET Word = N'óc lợn', [Action] = 'BLOCK', Severity = 'HIGH', IsActive = 1 WHERE NormalizedWord = N'oc lon';
ELSE
    INSERT INTO BannedWords (Word, NormalizedWord, [Action], Severity, IsActive, CreatedAt) VALUES (N'óc lợn', N'oc lon', 'BLOCK', 'HIGH', 1, GETDATE());
IF EXISTS (SELECT 1 FROM BannedWords WHERE NormalizedWord = N'oc heo')
    UPDATE BannedWords SET Word = N'óc heo', [Action] = 'BLOCK', Severity = 'HIGH', IsActive = 1 WHERE NormalizedWord = N'oc heo';
ELSE
    INSERT INTO BannedWords (Word, NormalizedWord, [Action], Severity, IsActive, CreatedAt) VALUES (N'óc heo', N'oc heo', 'BLOCK', 'HIGH', 1, GETDATE());
IF EXISTS (SELECT 1 FROM BannedWords WHERE NormalizedWord = N'do khon')
    UPDATE BannedWords SET Word = N'đồ khốn', [Action] = 'BLOCK', Severity = 'HIGH', IsActive = 1 WHERE NormalizedWord = N'do khon';
ELSE
    INSERT INTO BannedWords (Word, NormalizedWord, [Action], Severity, IsActive, CreatedAt) VALUES (N'đồ khốn', N'do khon', 'BLOCK', 'HIGH', 1, GETDATE());
IF EXISTS (SELECT 1 FROM BannedWords WHERE NormalizedWord = N'khon nan')
    UPDATE BannedWords SET Word = N'khốn nạn', [Action] = 'BLOCK', Severity = 'HIGH', IsActive = 1 WHERE NormalizedWord = N'khon nan';
ELSE
    INSERT INTO BannedWords (Word, NormalizedWord, [Action], Severity, IsActive, CreatedAt) VALUES (N'khốn nạn', N'khon nan', 'BLOCK', 'HIGH', 1, GETDATE());
IF EXISTS (SELECT 1 FROM BannedWords WHERE NormalizedWord = N'khon kiep')
    UPDATE BannedWords SET Word = N'khốn kiếp', [Action] = 'BLOCK', Severity = 'HIGH', IsActive = 1 WHERE NormalizedWord = N'khon kiep';
ELSE
    INSERT INTO BannedWords (Word, NormalizedWord, [Action], Severity, IsActive, CreatedAt) VALUES (N'khốn kiếp', N'khon kiep', 'BLOCK', 'HIGH', 1, GETDATE());
IF EXISTS (SELECT 1 FROM BannedWords WHERE NormalizedWord = N'do toi')
    UPDATE BannedWords SET Word = N'đồ tồi', [Action] = 'MASK', Severity = 'MEDIUM', IsActive = 1 WHERE NormalizedWord = N'do toi';
ELSE
    INSERT INTO BannedWords (Word, NormalizedWord, [Action], Severity, IsActive, CreatedAt) VALUES (N'đồ tồi', N'do toi', 'MASK', 'MEDIUM', 1, GETDATE());
IF EXISTS (SELECT 1 FROM BannedWords WHERE NormalizedWord = N'do hen')
    UPDATE BannedWords SET Word = N'đồ hèn', [Action] = 'MASK', Severity = 'MEDIUM', IsActive = 1 WHERE NormalizedWord = N'do hen';
ELSE
    INSERT INTO BannedWords (Word, NormalizedWord, [Action], Severity, IsActive, CreatedAt) VALUES (N'đồ hèn', N'do hen', 'MASK', 'MEDIUM', 1, GETDATE());
IF EXISTS (SELECT 1 FROM BannedWords WHERE NormalizedWord = N'do cho')
    UPDATE BannedWords SET Word = N'đồ chó', [Action] = 'BLOCK', Severity = 'HIGH', IsActive = 1 WHERE NormalizedWord = N'do cho';
ELSE
    INSERT INTO BannedWords (Word, NormalizedWord, [Action], Severity, IsActive, CreatedAt) VALUES (N'đồ chó', N'do cho', 'BLOCK', 'HIGH', 1, GETDATE());
IF EXISTS (SELECT 1 FROM BannedWords WHERE NormalizedWord = N'cho chet')
    UPDATE BannedWords SET Word = N'chó chết', [Action] = 'BLOCK', Severity = 'HIGH', IsActive = 1 WHERE NormalizedWord = N'cho chet';
ELSE
    INSERT INTO BannedWords (Word, NormalizedWord, [Action], Severity, IsActive, CreatedAt) VALUES (N'chó chết', N'cho chet', 'BLOCK', 'HIGH', 1, GETDATE());
IF EXISTS (SELECT 1 FROM BannedWords WHERE NormalizedWord = N'cho de')
    UPDATE BannedWords SET Word = N'chó đẻ', [Action] = 'BLOCK', Severity = 'HIGH', IsActive = 1 WHERE NormalizedWord = N'cho de';
ELSE
    INSERT INTO BannedWords (Word, NormalizedWord, [Action], Severity, IsActive, CreatedAt) VALUES (N'chó đẻ', N'cho de', 'BLOCK', 'HIGH', 1, GETDATE());
IF EXISTS (SELECT 1 FROM BannedWords WHERE NormalizedWord = N'con cho')
    UPDATE BannedWords SET Word = N'con chó', [Action] = 'BLOCK', Severity = 'HIGH', IsActive = 1 WHERE NormalizedWord = N'con cho';
ELSE
    INSERT INTO BannedWords (Word, NormalizedWord, [Action], Severity, IsActive, CreatedAt) VALUES (N'con chó', N'con cho', 'BLOCK', 'HIGH', 1, GETDATE());
IF EXISTS (SELECT 1 FROM BannedWords WHERE NormalizedWord = N'thang cho')
    UPDATE BannedWords SET Word = N'thằng chó', [Action] = 'BLOCK', Severity = 'HIGH', IsActive = 1 WHERE NormalizedWord = N'thang cho';
ELSE
    INSERT INTO BannedWords (Word, NormalizedWord, [Action], Severity, IsActive, CreatedAt) VALUES (N'thằng chó', N'thang cho', 'BLOCK', 'HIGH', 1, GETDATE());
IF EXISTS (SELECT 1 FROM BannedWords WHERE NormalizedWord = N'con lon')
    UPDATE BannedWords SET Word = N'con lợn', [Action] = 'MASK', Severity = 'MEDIUM', IsActive = 1 WHERE NormalizedWord = N'con lon';
ELSE
    INSERT INTO BannedWords (Word, NormalizedWord, [Action], Severity, IsActive, CreatedAt) VALUES (N'con lợn', N'con lon', 'MASK', 'MEDIUM', 1, GETDATE());
IF EXISTS (SELECT 1 FROM BannedWords WHERE NormalizedWord = N'do lon')
    UPDATE BannedWords SET Word = N'đồ lợn', [Action] = 'MASK', Severity = 'MEDIUM', IsActive = 1 WHERE NormalizedWord = N'do lon';
ELSE
    INSERT INTO BannedWords (Word, NormalizedWord, [Action], Severity, IsActive, CreatedAt) VALUES (N'đồ lợn', N'do lon', 'MASK', 'MEDIUM', 1, GETDATE());
IF EXISTS (SELECT 1 FROM BannedWords WHERE NormalizedWord = N'do heo')
    UPDATE BannedWords SET Word = N'đồ heo', [Action] = 'MASK', Severity = 'MEDIUM', IsActive = 1 WHERE NormalizedWord = N'do heo';
ELSE
    INSERT INTO BannedWords (Word, NormalizedWord, [Action], Severity, IsActive, CreatedAt) VALUES (N'đồ heo', N'do heo', 'MASK', 'MEDIUM', 1, GETDATE());
IF EXISTS (SELECT 1 FROM BannedWords WHERE NormalizedWord = N'do suc vat')
    UPDATE BannedWords SET Word = N'đồ súc vật', [Action] = 'BLOCK', Severity = 'HIGH', IsActive = 1 WHERE NormalizedWord = N'do suc vat';
ELSE
    INSERT INTO BannedWords (Word, NormalizedWord, [Action], Severity, IsActive, CreatedAt) VALUES (N'đồ súc vật', N'do suc vat', 'BLOCK', 'HIGH', 1, GETDATE());
IF EXISTS (SELECT 1 FROM BannedWords WHERE NormalizedWord = N'suc sinh')
    UPDATE BannedWords SET Word = N'súc sinh', [Action] = 'BLOCK', Severity = 'HIGH', IsActive = 1 WHERE NormalizedWord = N'suc sinh';
ELSE
    INSERT INTO BannedWords (Word, NormalizedWord, [Action], Severity, IsActive, CreatedAt) VALUES (N'súc sinh', N'suc sinh', 'BLOCK', 'HIGH', 1, GETDATE());
IF EXISTS (SELECT 1 FROM BannedWords WHERE NormalizedWord = N'cut di')
    UPDATE BannedWords SET Word = N'cút đi', [Action] = 'MASK', Severity = 'MEDIUM', IsActive = 1 WHERE NormalizedWord = N'cut di';
ELSE
    INSERT INTO BannedWords (Word, NormalizedWord, [Action], Severity, IsActive, CreatedAt) VALUES (N'cút đi', N'cut di', 'MASK', 'MEDIUM', 1, GETDATE());
IF EXISTS (SELECT 1 FROM BannedWords WHERE NormalizedWord = N'cut')
    UPDATE BannedWords SET Word = N'cứt', [Action] = 'MASK', Severity = 'MEDIUM', IsActive = 1 WHERE NormalizedWord = N'cut';
ELSE
    INSERT INTO BannedWords (Word, NormalizedWord, [Action], Severity, IsActive, CreatedAt) VALUES (N'cứt', N'cut', 'MASK', 'MEDIUM', 1, GETDATE());
IF EXISTS (SELECT 1 FROM BannedWords WHERE NormalizedWord = N'me may')
    UPDATE BannedWords SET Word = N'mẹ mày', [Action] = 'BLOCK', Severity = 'HIGH', IsActive = 1 WHERE NormalizedWord = N'me may';
ELSE
    INSERT INTO BannedWords (Word, NormalizedWord, [Action], Severity, IsActive, CreatedAt) VALUES (N'mẹ mày', N'me may', 'BLOCK', 'HIGH', 1, GETDATE());
IF EXISTS (SELECT 1 FROM BannedWords WHERE NormalizedWord = N'dit me')
    UPDATE BannedWords SET Word = N'địt mẹ', [Action] = 'BLOCK', Severity = 'HIGH', IsActive = 1 WHERE NormalizedWord = N'dit me';
ELSE
    INSERT INTO BannedWords (Word, NormalizedWord, [Action], Severity, IsActive, CreatedAt) VALUES (N'địt mẹ', N'dit me', 'BLOCK', 'HIGH', 1, GETDATE());
IF EXISTS (SELECT 1 FROM BannedWords WHERE NormalizedWord = N'dit')
    UPDATE BannedWords SET Word = N'địt', [Action] = 'BLOCK', Severity = 'HIGH', IsActive = 1 WHERE NormalizedWord = N'dit';
ELSE
    INSERT INTO BannedWords (Word, NormalizedWord, [Action], Severity, IsActive, CreatedAt) VALUES (N'địt', N'dit', 'BLOCK', 'HIGH', 1, GETDATE());
IF EXISTS (SELECT 1 FROM BannedWords WHERE NormalizedWord = N'du me')
    UPDATE BannedWords SET Word = N'đụ mẹ', [Action] = 'BLOCK', Severity = 'HIGH', IsActive = 1 WHERE NormalizedWord = N'du me';
ELSE
    INSERT INTO BannedWords (Word, NormalizedWord, [Action], Severity, IsActive, CreatedAt) VALUES (N'đụ mẹ', N'du me', 'BLOCK', 'HIGH', 1, GETDATE());
IF EXISTS (SELECT 1 FROM BannedWords WHERE NormalizedWord = N'du')
    UPDATE BannedWords SET Word = N'đụ', [Action] = 'BLOCK', Severity = 'HIGH', IsActive = 1 WHERE NormalizedWord = N'du';
ELSE
    INSERT INTO BannedWords (Word, NormalizedWord, [Action], Severity, IsActive, CreatedAt) VALUES (N'đụ', N'du', 'BLOCK', 'HIGH', 1, GETDATE());
IF EXISTS (SELECT 1 FROM BannedWords WHERE NormalizedWord = N'deo')
    UPDATE BannedWords SET Word = N'đéo', [Action] = 'BLOCK', Severity = 'HIGH', IsActive = 1 WHERE NormalizedWord = N'deo';
ELSE
    INSERT INTO BannedWords (Word, NormalizedWord, [Action], Severity, IsActive, CreatedAt) VALUES (N'đéo', N'deo', 'BLOCK', 'HIGH', 1, GETDATE());
IF EXISTS (SELECT 1 FROM BannedWords WHERE NormalizedWord = N'vcl')
    UPDATE BannedWords SET Word = N'vcl', [Action] = 'BLOCK', Severity = 'HIGH', IsActive = 1 WHERE NormalizedWord = N'vcl';
ELSE
    INSERT INTO BannedWords (Word, NormalizedWord, [Action], Severity, IsActive, CreatedAt) VALUES (N'vcl', N'vcl', 'BLOCK', 'HIGH', 1, GETDATE());
IF EXISTS (SELECT 1 FROM BannedWords WHERE NormalizedWord = N'vl')
    UPDATE BannedWords SET Word = N'vl', [Action] = 'MASK', Severity = 'MEDIUM', IsActive = 1 WHERE NormalizedWord = N'vl';
ELSE
    INSERT INTO BannedWords (Word, NormalizedWord, [Action], Severity, IsActive, CreatedAt) VALUES (N'vl', N'vl', 'MASK', 'MEDIUM', 1, GETDATE());
IF EXISTS (SELECT 1 FROM BannedWords WHERE NormalizedWord = N'vai lon')
    UPDATE BannedWords SET Word = N'vãi lồn', [Action] = 'BLOCK', Severity = 'HIGH', IsActive = 1 WHERE NormalizedWord = N'vai lon';
ELSE
    INSERT INTO BannedWords (Word, NormalizedWord, [Action], Severity, IsActive, CreatedAt) VALUES (N'vãi lồn', N'vai lon', 'BLOCK', 'HIGH', 1, GETDATE());
IF EXISTS (SELECT 1 FROM BannedWords WHERE NormalizedWord = N'vai dai')
    UPDATE BannedWords SET Word = N'vãi đái', [Action] = 'MASK', Severity = 'MEDIUM', IsActive = 1 WHERE NormalizedWord = N'vai dai';
ELSE
    INSERT INTO BannedWords (Word, NormalizedWord, [Action], Severity, IsActive, CreatedAt) VALUES (N'vãi đái', N'vai dai', 'MASK', 'MEDIUM', 1, GETDATE());
IF EXISTS (SELECT 1 FROM BannedWords WHERE NormalizedWord = N'vai cut')
    UPDATE BannedWords SET Word = N'vãi cứt', [Action] = 'MASK', Severity = 'MEDIUM', IsActive = 1 WHERE NormalizedWord = N'vai cut';
ELSE
    INSERT INTO BannedWords (Word, NormalizedWord, [Action], Severity, IsActive, CreatedAt) VALUES (N'vãi cứt', N'vai cut', 'MASK', 'MEDIUM', 1, GETDATE());
IF EXISTS (SELECT 1 FROM BannedWords WHERE NormalizedWord = N'dai')
    UPDATE BannedWords SET Word = N'đái', [Action] = 'MASK', Severity = 'MEDIUM', IsActive = 1 WHERE NormalizedWord = N'dai';
ELSE
    INSERT INTO BannedWords (Word, NormalizedWord, [Action], Severity, IsActive, CreatedAt) VALUES (N'đái', N'dai', 'MASK', 'MEDIUM', 1, GETDATE());
IF EXISTS (SELECT 1 FROM BannedWords WHERE NormalizedWord = N'lon')
    UPDATE BannedWords SET Word = N'lồn', [Action] = 'BLOCK', Severity = 'HIGH', IsActive = 1 WHERE NormalizedWord = N'lon';
ELSE
    INSERT INTO BannedWords (Word, NormalizedWord, [Action], Severity, IsActive, CreatedAt) VALUES (N'lồn', N'lon', 'BLOCK', 'HIGH', 1, GETDATE());
IF EXISTS (SELECT 1 FROM BannedWords WHERE NormalizedWord = N'lua dao')
    UPDATE BannedWords SET Word = N'lua dao', [Action] = 'BLOCK', Severity = 'HIGH', IsActive = 1 WHERE NormalizedWord = N'lua dao';
ELSE
    INSERT INTO BannedWords (Word, NormalizedWord, [Action], Severity, IsActive, CreatedAt) VALUES (N'lua dao', N'lua dao', 'BLOCK', 'HIGH', 1, GETDATE());
IF EXISTS (SELECT 1 FROM BannedWords WHERE NormalizedWord = N'lua gat')
    UPDATE BannedWords SET Word = N'lừa gạt', [Action] = 'BLOCK', Severity = 'HIGH', IsActive = 1 WHERE NormalizedWord = N'lua gat';
ELSE
    INSERT INTO BannedWords (Word, NormalizedWord, [Action], Severity, IsActive, CreatedAt) VALUES (N'lừa gạt', N'lua gat', 'BLOCK', 'HIGH', 1, GETDATE());
IF EXISTS (SELECT 1 FROM BannedWords WHERE NormalizedWord = N'hang gia')
    UPDATE BannedWords SET Word = N'hàng giả', [Action] = 'BLOCK', Severity = 'HIGH', IsActive = 1 WHERE NormalizedWord = N'hang gia';
ELSE
    INSERT INTO BannedWords (Word, NormalizedWord, [Action], Severity, IsActive, CreatedAt) VALUES (N'hàng giả', N'hang gia', 'BLOCK', 'HIGH', 1, GETDATE());
IF EXISTS (SELECT 1 FROM BannedWords WHERE NormalizedWord = N'hang nhai')
    UPDATE BannedWords SET Word = N'hàng nhái', [Action] = 'BLOCK', Severity = 'HIGH', IsActive = 1 WHERE NormalizedWord = N'hang nhai';
ELSE
    INSERT INTO BannedWords (Word, NormalizedWord, [Action], Severity, IsActive, CreatedAt) VALUES (N'hàng nhái', N'hang nhai', 'BLOCK', 'HIGH', 1, GETDATE());
IF EXISTS (SELECT 1 FROM BannedWords WHERE NormalizedWord = N'hang fake')
    UPDATE BannedWords SET Word = N'hàng fake', [Action] = 'BLOCK', Severity = 'HIGH', IsActive = 1 WHERE NormalizedWord = N'hang fake';
ELSE
    INSERT INTO BannedWords (Word, NormalizedWord, [Action], Severity, IsActive, CreatedAt) VALUES (N'hàng fake', N'hang fake', 'BLOCK', 'HIGH', 1, GETDATE());
IF EXISTS (SELECT 1 FROM BannedWords WHERE NormalizedWord = N'hang kem chat luong')
    UPDATE BannedWords SET Word = N'hàng kém chất lượng', [Action] = 'MASK', Severity = 'MEDIUM', IsActive = 1 WHERE NormalizedWord = N'hang kem chat luong';
ELSE
    INSERT INTO BannedWords (Word, NormalizedWord, [Action], Severity, IsActive, CreatedAt) VALUES (N'hàng kém chất lượng', N'hang kem chat luong', 'MASK', 'MEDIUM', 1, GETDATE());
IF EXISTS (SELECT 1 FROM BannedWords WHERE NormalizedWord = N'hang nat')
    UPDATE BannedWords SET Word = N'hàng nát', [Action] = 'MASK', Severity = 'MEDIUM', IsActive = 1 WHERE NormalizedWord = N'hang nat';
ELSE
    INSERT INTO BannedWords (Word, NormalizedWord, [Action], Severity, IsActive, CreatedAt) VALUES (N'hàng nát', N'hang nat', 'MASK', 'MEDIUM', 1, GETDATE());
IF EXISTS (SELECT 1 FROM BannedWords WHERE NormalizedWord = N'bung hang')
    UPDATE BannedWords SET Word = N'bùng hàng', [Action] = 'BLOCK', Severity = 'HIGH', IsActive = 1 WHERE NormalizedWord = N'bung hang';
ELSE
    INSERT INTO BannedWords (Word, NormalizedWord, [Action], Severity, IsActive, CreatedAt) VALUES (N'bùng hàng', N'bung hang', 'BLOCK', 'HIGH', 1, GETDATE());
IF EXISTS (SELECT 1 FROM BannedWords WHERE NormalizedWord = N'bom hang')
    UPDATE BannedWords SET Word = N'bom hàng', [Action] = 'BLOCK', Severity = 'HIGH', IsActive = 1 WHERE NormalizedWord = N'bom hang';
ELSE
    INSERT INTO BannedWords (Word, NormalizedWord, [Action], Severity, IsActive, CreatedAt) VALUES (N'bom hàng', N'bom hang', 'BLOCK', 'HIGH', 1, GETDATE());
IF EXISTS (SELECT 1 FROM BannedWords WHERE NormalizedWord = N'quyt tien')
    UPDATE BannedWords SET Word = N'quỵt tiền', [Action] = 'BLOCK', Severity = 'HIGH', IsActive = 1 WHERE NormalizedWord = N'quyt tien';
ELSE
    INSERT INTO BannedWords (Word, NormalizedWord, [Action], Severity, IsActive, CreatedAt) VALUES (N'quỵt tiền', N'quyt tien', 'BLOCK', 'HIGH', 1, GETDATE());
IF EXISTS (SELECT 1 FROM BannedWords WHERE NormalizedWord = N'an chan')
    UPDATE BannedWords SET Word = N'ăn chặn', [Action] = 'BLOCK', Severity = 'HIGH', IsActive = 1 WHERE NormalizedWord = N'an chan';
ELSE
    INSERT INTO BannedWords (Word, NormalizedWord, [Action], Severity, IsActive, CreatedAt) VALUES (N'ăn chặn', N'an chan', 'BLOCK', 'HIGH', 1, GETDATE());
IF EXISTS (SELECT 1 FROM BannedWords WHERE NormalizedWord = N'an cap')
    UPDATE BannedWords SET Word = N'ăn cắp', [Action] = 'BLOCK', Severity = 'HIGH', IsActive = 1 WHERE NormalizedWord = N'an cap';
ELSE
    INSERT INTO BannedWords (Word, NormalizedWord, [Action], Severity, IsActive, CreatedAt) VALUES (N'ăn cắp', N'an cap', 'BLOCK', 'HIGH', 1, GETDATE());
IF EXISTS (SELECT 1 FROM BannedWords WHERE NormalizedWord = N'trom cap')
    UPDATE BannedWords SET Word = N'trộm cắp', [Action] = 'BLOCK', Severity = 'HIGH', IsActive = 1 WHERE NormalizedWord = N'trom cap';
ELSE
    INSERT INTO BannedWords (Word, NormalizedWord, [Action], Severity, IsActive, CreatedAt) VALUES (N'trộm cắp', N'trom cap', 'BLOCK', 'HIGH', 1, GETDATE());
IF EXISTS (SELECT 1 FROM BannedWords WHERE NormalizedWord = N'cuop giat')
    UPDATE BannedWords SET Word = N'cướp giật', [Action] = 'BLOCK', Severity = 'HIGH', IsActive = 1 WHERE NormalizedWord = N'cuop giat';
ELSE
    INSERT INTO BannedWords (Word, NormalizedWord, [Action], Severity, IsActive, CreatedAt) VALUES (N'cướp giật', N'cuop giat', 'BLOCK', 'HIGH', 1, GETDATE());
IF EXISTS (SELECT 1 FROM BannedWords WHERE NormalizedWord = N'co bac')
    UPDATE BannedWords SET Word = N'cờ bạc', [Action] = 'BLOCK', Severity = 'HIGH', IsActive = 1 WHERE NormalizedWord = N'co bac';
ELSE
    INSERT INTO BannedWords (Word, NormalizedWord, [Action], Severity, IsActive, CreatedAt) VALUES (N'cờ bạc', N'co bac', 'BLOCK', 'HIGH', 1, GETDATE());
IF EXISTS (SELECT 1 FROM BannedWords WHERE NormalizedWord = N'ca do')
    UPDATE BannedWords SET Word = N'cá độ', [Action] = 'BLOCK', Severity = 'HIGH', IsActive = 1 WHERE NormalizedWord = N'ca do';
ELSE
    INSERT INTO BannedWords (Word, NormalizedWord, [Action], Severity, IsActive, CreatedAt) VALUES (N'cá độ', N'ca do', 'BLOCK', 'HIGH', 1, GETDATE());
IF EXISTS (SELECT 1 FROM BannedWords WHERE NormalizedWord = N'vay nang lai')
    UPDATE BannedWords SET Word = N'vay nặng lãi', [Action] = 'BLOCK', Severity = 'HIGH', IsActive = 1 WHERE NormalizedWord = N'vay nang lai';
ELSE
    INSERT INTO BannedWords (Word, NormalizedWord, [Action], Severity, IsActive, CreatedAt) VALUES (N'vay nặng lãi', N'vay nang lai', 'BLOCK', 'HIGH', 1, GETDATE());
IF EXISTS (SELECT 1 FROM BannedWords WHERE NormalizedWord = N'tin dung den')
    UPDATE BannedWords SET Word = N'tín dụng đen', [Action] = 'BLOCK', Severity = 'HIGH', IsActive = 1 WHERE NormalizedWord = N'tin dung den';
ELSE
    INSERT INTO BannedWords (Word, NormalizedWord, [Action], Severity, IsActive, CreatedAt) VALUES (N'tín dụng đen', N'tin dung den', 'BLOCK', 'HIGH', 1, GETDATE());
IF EXISTS (SELECT 1 FROM BannedWords WHERE NormalizedWord = N'da cap')
    UPDATE BannedWords SET Word = N'đa cấp', [Action] = 'MASK', Severity = 'MEDIUM', IsActive = 1 WHERE NormalizedWord = N'da cap';
ELSE
    INSERT INTO BannedWords (Word, NormalizedWord, [Action], Severity, IsActive, CreatedAt) VALUES (N'đa cấp', N'da cap', 'MASK', 'MEDIUM', 1, GETDATE());
IF EXISTS (SELECT 1 FROM BannedWords WHERE NormalizedWord = N'spam')
    UPDATE BannedWords SET Word = N'spam', [Action] = 'MASK', Severity = 'MEDIUM', IsActive = 1 WHERE NormalizedWord = N'spam';
ELSE
    INSERT INTO BannedWords (Word, NormalizedWord, [Action], Severity, IsActive, CreatedAt) VALUES (N'spam', N'spam', 'MASK', 'MEDIUM', 1, GETDATE());
IF EXISTS (SELECT 1 FROM BannedWords WHERE NormalizedWord = N'seeding')
    UPDATE BannedWords SET Word = N'seeding', [Action] = 'MASK', Severity = 'MEDIUM', IsActive = 1 WHERE NormalizedWord = N'seeding';
ELSE
    INSERT INTO BannedWords (Word, NormalizedWord, [Action], Severity, IsActive, CreatedAt) VALUES (N'seeding', N'seeding', 'MASK', 'MEDIUM', 1, GETDATE());
IF EXISTS (SELECT 1 FROM BannedWords WHERE NormalizedWord = N'nham nhi')
    UPDATE BannedWords SET Word = N'nhảm nhí', [Action] = 'MASK', Severity = 'MEDIUM', IsActive = 1 WHERE NormalizedWord = N'nham nhi';
ELSE
    INSERT INTO BannedWords (Word, NormalizedWord, [Action], Severity, IsActive, CreatedAt) VALUES (N'nhảm nhí', N'nham nhi', 'MASK', 'MEDIUM', 1, GETDATE());
IF EXISTS (SELECT 1 FROM BannedWords WHERE NormalizedWord = N'vo hoc')
    UPDATE BannedWords SET Word = N'vô học', [Action] = 'MASK', Severity = 'MEDIUM', IsActive = 1 WHERE NormalizedWord = N'vo hoc';
ELSE
    INSERT INTO BannedWords (Word, NormalizedWord, [Action], Severity, IsActive, CreatedAt) VALUES (N'vô học', N'vo hoc', 'MASK', 'MEDIUM', 1, GETDATE());
IF EXISTS (SELECT 1 FROM BannedWords WHERE NormalizedWord = N'mat day')
    UPDATE BannedWords SET Word = N'mất dạy', [Action] = 'MASK', Severity = 'MEDIUM', IsActive = 1 WHERE NormalizedWord = N'mat day';
ELSE
    INSERT INTO BannedWords (Word, NormalizedWord, [Action], Severity, IsActive, CreatedAt) VALUES (N'mất dạy', N'mat day', 'MASK', 'MEDIUM', 1, GETDATE());
IF EXISTS (SELECT 1 FROM BannedWords WHERE NormalizedWord = N'fuck')
    UPDATE BannedWords SET Word = N'fuck', [Action] = 'BLOCK', Severity = 'HIGH', IsActive = 1 WHERE NormalizedWord = N'fuck';
ELSE
    INSERT INTO BannedWords (Word, NormalizedWord, [Action], Severity, IsActive, CreatedAt) VALUES (N'fuck', N'fuck', 'BLOCK', 'HIGH', 1, GETDATE());
IF EXISTS (SELECT 1 FROM BannedWords WHERE NormalizedWord = N'fucking')
    UPDATE BannedWords SET Word = N'fucking', [Action] = 'BLOCK', Severity = 'HIGH', IsActive = 1 WHERE NormalizedWord = N'fucking';
ELSE
    INSERT INTO BannedWords (Word, NormalizedWord, [Action], Severity, IsActive, CreatedAt) VALUES (N'fucking', N'fucking', 'BLOCK', 'HIGH', 1, GETDATE());
IF EXISTS (SELECT 1 FROM BannedWords WHERE NormalizedWord = N'fucked')
    UPDATE BannedWords SET Word = N'fucked', [Action] = 'BLOCK', Severity = 'HIGH', IsActive = 1 WHERE NormalizedWord = N'fucked';
ELSE
    INSERT INTO BannedWords (Word, NormalizedWord, [Action], Severity, IsActive, CreatedAt) VALUES (N'fucked', N'fucked', 'BLOCK', 'HIGH', 1, GETDATE());
IF EXISTS (SELECT 1 FROM BannedWords WHERE NormalizedWord = N'motherfucker')
    UPDATE BannedWords SET Word = N'motherfucker', [Action] = 'BLOCK', Severity = 'HIGH', IsActive = 1 WHERE NormalizedWord = N'motherfucker';
ELSE
    INSERT INTO BannedWords (Word, NormalizedWord, [Action], Severity, IsActive, CreatedAt) VALUES (N'motherfucker', N'motherfucker', 'BLOCK', 'HIGH', 1, GETDATE());
IF EXISTS (SELECT 1 FROM BannedWords WHERE NormalizedWord = N'shit')
    UPDATE BannedWords SET Word = N'shit', [Action] = 'BLOCK', Severity = 'HIGH', IsActive = 1 WHERE NormalizedWord = N'shit';
ELSE
    INSERT INTO BannedWords (Word, NormalizedWord, [Action], Severity, IsActive, CreatedAt) VALUES (N'shit', N'shit', 'BLOCK', 'HIGH', 1, GETDATE());
IF EXISTS (SELECT 1 FROM BannedWords WHERE NormalizedWord = N'bullshit')
    UPDATE BannedWords SET Word = N'bullshit', [Action] = 'BLOCK', Severity = 'HIGH', IsActive = 1 WHERE NormalizedWord = N'bullshit';
ELSE
    INSERT INTO BannedWords (Word, NormalizedWord, [Action], Severity, IsActive, CreatedAt) VALUES (N'bullshit', N'bullshit', 'BLOCK', 'HIGH', 1, GETDATE());
IF EXISTS (SELECT 1 FROM BannedWords WHERE NormalizedWord = N'bitch')
    UPDATE BannedWords SET Word = N'bitch', [Action] = 'BLOCK', Severity = 'HIGH', IsActive = 1 WHERE NormalizedWord = N'bitch';
ELSE
    INSERT INTO BannedWords (Word, NormalizedWord, [Action], Severity, IsActive, CreatedAt) VALUES (N'bitch', N'bitch', 'BLOCK', 'HIGH', 1, GETDATE());
IF EXISTS (SELECT 1 FROM BannedWords WHERE NormalizedWord = N'asshole')
    UPDATE BannedWords SET Word = N'asshole', [Action] = 'BLOCK', Severity = 'HIGH', IsActive = 1 WHERE NormalizedWord = N'asshole';
ELSE
    INSERT INTO BannedWords (Word, NormalizedWord, [Action], Severity, IsActive, CreatedAt) VALUES (N'asshole', N'asshole', 'BLOCK', 'HIGH', 1, GETDATE());
IF EXISTS (SELECT 1 FROM BannedWords WHERE NormalizedWord = N'bastard')
    UPDATE BannedWords SET Word = N'bastard', [Action] = 'BLOCK', Severity = 'HIGH', IsActive = 1 WHERE NormalizedWord = N'bastard';
ELSE
    INSERT INTO BannedWords (Word, NormalizedWord, [Action], Severity, IsActive, CreatedAt) VALUES (N'bastard', N'bastard', 'BLOCK', 'HIGH', 1, GETDATE());
IF EXISTS (SELECT 1 FROM BannedWords WHERE NormalizedWord = N'dick')
    UPDATE BannedWords SET Word = N'dick', [Action] = 'BLOCK', Severity = 'HIGH', IsActive = 1 WHERE NormalizedWord = N'dick';
ELSE
    INSERT INTO BannedWords (Word, NormalizedWord, [Action], Severity, IsActive, CreatedAt) VALUES (N'dick', N'dick', 'BLOCK', 'HIGH', 1, GETDATE());
IF EXISTS (SELECT 1 FROM BannedWords WHERE NormalizedWord = N'pussy')
    UPDATE BannedWords SET Word = N'pussy', [Action] = 'BLOCK', Severity = 'HIGH', IsActive = 1 WHERE NormalizedWord = N'pussy';
ELSE
    INSERT INTO BannedWords (Word, NormalizedWord, [Action], Severity, IsActive, CreatedAt) VALUES (N'pussy', N'pussy', 'BLOCK', 'HIGH', 1, GETDATE());
IF EXISTS (SELECT 1 FROM BannedWords WHERE NormalizedWord = N'cock')
    UPDATE BannedWords SET Word = N'cock', [Action] = 'BLOCK', Severity = 'HIGH', IsActive = 1 WHERE NormalizedWord = N'cock';
ELSE
    INSERT INTO BannedWords (Word, NormalizedWord, [Action], Severity, IsActive, CreatedAt) VALUES (N'cock', N'cock', 'BLOCK', 'HIGH', 1, GETDATE());
IF EXISTS (SELECT 1 FROM BannedWords WHERE NormalizedWord = N'whore')
    UPDATE BannedWords SET Word = N'whore', [Action] = 'BLOCK', Severity = 'HIGH', IsActive = 1 WHERE NormalizedWord = N'whore';
ELSE
    INSERT INTO BannedWords (Word, NormalizedWord, [Action], Severity, IsActive, CreatedAt) VALUES (N'whore', N'whore', 'BLOCK', 'HIGH', 1, GETDATE());
IF EXISTS (SELECT 1 FROM BannedWords WHERE NormalizedWord = N'slut')
    UPDATE BannedWords SET Word = N'slut', [Action] = 'BLOCK', Severity = 'HIGH', IsActive = 1 WHERE NormalizedWord = N'slut';
ELSE
    INSERT INTO BannedWords (Word, NormalizedWord, [Action], Severity, IsActive, CreatedAt) VALUES (N'slut', N'slut', 'BLOCK', 'HIGH', 1, GETDATE());
IF EXISTS (SELECT 1 FROM BannedWords WHERE NormalizedWord = N'idiot')
    UPDATE BannedWords SET Word = N'idiot', [Action] = 'MASK', Severity = 'MEDIUM', IsActive = 1 WHERE NormalizedWord = N'idiot';
ELSE
    INSERT INTO BannedWords (Word, NormalizedWord, [Action], Severity, IsActive, CreatedAt) VALUES (N'idiot', N'idiot', 'MASK', 'MEDIUM', 1, GETDATE());
IF EXISTS (SELECT 1 FROM BannedWords WHERE NormalizedWord = N'stupid')
    UPDATE BannedWords SET Word = N'stupid', [Action] = 'MASK', Severity = 'MEDIUM', IsActive = 1 WHERE NormalizedWord = N'stupid';
ELSE
    INSERT INTO BannedWords (Word, NormalizedWord, [Action], Severity, IsActive, CreatedAt) VALUES (N'stupid', N'stupid', 'MASK', 'MEDIUM', 1, GETDATE());
IF EXISTS (SELECT 1 FROM BannedWords WHERE NormalizedWord = N'moron')
    UPDATE BannedWords SET Word = N'moron', [Action] = 'MASK', Severity = 'MEDIUM', IsActive = 1 WHERE NormalizedWord = N'moron';
ELSE
    INSERT INTO BannedWords (Word, NormalizedWord, [Action], Severity, IsActive, CreatedAt) VALUES (N'moron', N'moron', 'MASK', 'MEDIUM', 1, GETDATE());
IF EXISTS (SELECT 1 FROM BannedWords WHERE NormalizedWord = N'dumb')
    UPDATE BannedWords SET Word = N'dumb', [Action] = 'MASK', Severity = 'MEDIUM', IsActive = 1 WHERE NormalizedWord = N'dumb';
ELSE
    INSERT INTO BannedWords (Word, NormalizedWord, [Action], Severity, IsActive, CreatedAt) VALUES (N'dumb', N'dumb', 'MASK', 'MEDIUM', 1, GETDATE());
IF EXISTS (SELECT 1 FROM BannedWords WHERE NormalizedWord = N'loser')
    UPDATE BannedWords SET Word = N'loser', [Action] = 'MASK', Severity = 'MEDIUM', IsActive = 1 WHERE NormalizedWord = N'loser';
ELSE
    INSERT INTO BannedWords (Word, NormalizedWord, [Action], Severity, IsActive, CreatedAt) VALUES (N'loser', N'loser', 'MASK', 'MEDIUM', 1, GETDATE());
IF EXISTS (SELECT 1 FROM BannedWords WHERE NormalizedWord = N'trash')
    UPDATE BannedWords SET Word = N'trash', [Action] = 'MASK', Severity = 'MEDIUM', IsActive = 1 WHERE NormalizedWord = N'trash';
ELSE
    INSERT INTO BannedWords (Word, NormalizedWord, [Action], Severity, IsActive, CreatedAt) VALUES (N'trash', N'trash', 'MASK', 'MEDIUM', 1, GETDATE());
IF EXISTS (SELECT 1 FROM BannedWords WHERE NormalizedWord = N'garbage')
    UPDATE BannedWords SET Word = N'garbage', [Action] = 'MASK', Severity = 'MEDIUM', IsActive = 1 WHERE NormalizedWord = N'garbage';
ELSE
    INSERT INTO BannedWords (Word, NormalizedWord, [Action], Severity, IsActive, CreatedAt) VALUES (N'garbage', N'garbage', 'MASK', 'MEDIUM', 1, GETDATE());
IF EXISTS (SELECT 1 FROM BannedWords WHERE NormalizedWord = N'scam')
    UPDATE BannedWords SET Word = N'scam', [Action] = 'BLOCK', Severity = 'HIGH', IsActive = 1 WHERE NormalizedWord = N'scam';
ELSE
    INSERT INTO BannedWords (Word, NormalizedWord, [Action], Severity, IsActive, CreatedAt) VALUES (N'scam', N'scam', 'BLOCK', 'HIGH', 1, GETDATE());
IF EXISTS (SELECT 1 FROM BannedWords WHERE NormalizedWord = N'scammer')
    UPDATE BannedWords SET Word = N'scammer', [Action] = 'BLOCK', Severity = 'HIGH', IsActive = 1 WHERE NormalizedWord = N'scammer';
ELSE
    INSERT INTO BannedWords (Word, NormalizedWord, [Action], Severity, IsActive, CreatedAt) VALUES (N'scammer', N'scammer', 'BLOCK', 'HIGH', 1, GETDATE());
IF EXISTS (SELECT 1 FROM BannedWords WHERE NormalizedWord = N'fraud')
    UPDATE BannedWords SET Word = N'fraud', [Action] = 'BLOCK', Severity = 'HIGH', IsActive = 1 WHERE NormalizedWord = N'fraud';
ELSE
    INSERT INTO BannedWords (Word, NormalizedWord, [Action], Severity, IsActive, CreatedAt) VALUES (N'fraud', N'fraud', 'BLOCK', 'HIGH', 1, GETDATE());
IF EXISTS (SELECT 1 FROM BannedWords WHERE NormalizedWord = N'fake')
    UPDATE BannedWords SET Word = N'fake', [Action] = 'MASK', Severity = 'MEDIUM', IsActive = 1 WHERE NormalizedWord = N'fake';
ELSE
    INSERT INTO BannedWords (Word, NormalizedWord, [Action], Severity, IsActive, CreatedAt) VALUES (N'fake', N'fake', 'MASK', 'MEDIUM', 1, GETDATE());
IF EXISTS (SELECT 1 FROM BannedWords WHERE NormalizedWord = N'counterfeit')
    UPDATE BannedWords SET Word = N'counterfeit', [Action] = 'BLOCK', Severity = 'HIGH', IsActive = 1 WHERE NormalizedWord = N'counterfeit';
ELSE
    INSERT INTO BannedWords (Word, NormalizedWord, [Action], Severity, IsActive, CreatedAt) VALUES (N'counterfeit', N'counterfeit', 'BLOCK', 'HIGH', 1, GETDATE());
IF EXISTS (SELECT 1 FROM BannedWords WHERE NormalizedWord = N'phishing')
    UPDATE BannedWords SET Word = N'phishing', [Action] = 'BLOCK', Severity = 'HIGH', IsActive = 1 WHERE NormalizedWord = N'phishing';
ELSE
    INSERT INTO BannedWords (Word, NormalizedWord, [Action], Severity, IsActive, CreatedAt) VALUES (N'phishing', N'phishing', 'BLOCK', 'HIGH', 1, GETDATE());
IF EXISTS (SELECT 1 FROM BannedWords WHERE NormalizedWord = N'malware')
    UPDATE BannedWords SET Word = N'malware', [Action] = 'BLOCK', Severity = 'HIGH', IsActive = 1 WHERE NormalizedWord = N'malware';
ELSE
    INSERT INTO BannedWords (Word, NormalizedWord, [Action], Severity, IsActive, CreatedAt) VALUES (N'malware', N'malware', 'BLOCK', 'HIGH', 1, GETDATE());
IF EXISTS (SELECT 1 FROM BannedWords WHERE NormalizedWord = N'virus')
    UPDATE BannedWords SET Word = N'virus', [Action] = 'BLOCK', Severity = 'HIGH', IsActive = 1 WHERE NormalizedWord = N'virus';
ELSE
    INSERT INTO BannedWords (Word, NormalizedWord, [Action], Severity, IsActive, CreatedAt) VALUES (N'virus', N'virus', 'BLOCK', 'HIGH', 1, GETDATE());
IF EXISTS (SELECT 1 FROM BannedWords WHERE NormalizedWord = N'hack')
    UPDATE BannedWords SET Word = N'hack', [Action] = 'BLOCK', Severity = 'HIGH', IsActive = 1 WHERE NormalizedWord = N'hack';
ELSE
    INSERT INTO BannedWords (Word, NormalizedWord, [Action], Severity, IsActive, CreatedAt) VALUES (N'hack', N'hack', 'BLOCK', 'HIGH', 1, GETDATE());
IF EXISTS (SELECT 1 FROM BannedWords WHERE NormalizedWord = N'hacker')
    UPDATE BannedWords SET Word = N'hacker', [Action] = 'BLOCK', Severity = 'HIGH', IsActive = 1 WHERE NormalizedWord = N'hacker';
ELSE
    INSERT INTO BannedWords (Word, NormalizedWord, [Action], Severity, IsActive, CreatedAt) VALUES (N'hacker', N'hacker', 'BLOCK', 'HIGH', 1, GETDATE());
IF EXISTS (SELECT 1 FROM BannedWords WHERE NormalizedWord = N'porn')
    UPDATE BannedWords SET Word = N'porn', [Action] = 'BLOCK', Severity = 'HIGH', IsActive = 1 WHERE NormalizedWord = N'porn';
ELSE
    INSERT INTO BannedWords (Word, NormalizedWord, [Action], Severity, IsActive, CreatedAt) VALUES (N'porn', N'porn', 'BLOCK', 'HIGH', 1, GETDATE());
IF EXISTS (SELECT 1 FROM BannedWords WHERE NormalizedWord = N'nsfw')
    UPDATE BannedWords SET Word = N'nsfw', [Action] = 'BLOCK', Severity = 'HIGH', IsActive = 1 WHERE NormalizedWord = N'nsfw';
ELSE
    INSERT INTO BannedWords (Word, NormalizedWord, [Action], Severity, IsActive, CreatedAt) VALUES (N'nsfw', N'nsfw', 'BLOCK', 'HIGH', 1, GETDATE());
IF EXISTS (SELECT 1 FROM BannedWords WHERE NormalizedWord = N'kill yourself')
    UPDATE BannedWords SET Word = N'kill yourself', [Action] = 'BLOCK', Severity = 'HIGH', IsActive = 1 WHERE NormalizedWord = N'kill yourself';
ELSE
    INSERT INTO BannedWords (Word, NormalizedWord, [Action], Severity, IsActive, CreatedAt) VALUES (N'kill yourself', N'kill yourself', 'BLOCK', 'HIGH', 1, GETDATE());
IF EXISTS (SELECT 1 FROM BannedWords WHERE NormalizedWord = N'kys')
    UPDATE BannedWords SET Word = N'kys', [Action] = 'BLOCK', Severity = 'HIGH', IsActive = 1 WHERE NormalizedWord = N'kys';
ELSE
    INSERT INTO BannedWords (Word, NormalizedWord, [Action], Severity, IsActive, CreatedAt) VALUES (N'kys', N'kys', 'BLOCK', 'HIGH', 1, GETDATE());
SELECT COUNT(*) AS TotalBannedWords FROM BannedWords;
SELECT TOP 15 WordId, Word, NormalizedWord, [Action] FROM BannedWords ORDER BY WordId DESC;

GO

-- >>> NGUON GOP: scripts\seed-test-accounts.sql <<<
-- Script tao them tai khoan STAFF va CUSTOMER de test (D:\clothing-shop-main)
-- Kiem tra ten bang va ten cot thuc te trong SQL Server truoc khi chay (vi du: Users, Roles, UserId, RoleId, FullName, Email, PasswordHash, IsActive)

DECLARE @StaffRoleId INT = (SELECT TOP 1 RoleId FROM Roles WHERE UPPER(RoleName) = 'STAFF');
DECLARE @CustomerRoleId INT = (SELECT TOP 1 RoleId FROM Roles WHERE UPPER(RoleName) = 'CUSTOMER');

-- Hash BCrypt cua "AdminShop123@" (dung BCryptPasswordEncoder cua Spring Security)
DECLARE @DefaultHash NVARCHAR(255) = '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5g0k5x6Q9yW1X6G9Z0yX2k3L4m5N6';

-- 1. Them cac tai khoan NHAN VIEN (STAFF)
IF @StaffRoleId IS NOT NULL
BEGIN
    IF NOT EXISTS (SELECT 1 FROM Users WHERE LOWER(Email) = LOWER('staff.kho@clothingshop.local'))
        INSERT INTO Users (FullName, Email, PasswordHash, RoleId, IsActive)
        VALUES (N'Nhân viên Kho', 'staff.kho@clothingshop.local', @DefaultHash, @StaffRoleId, 1);

    IF NOT EXISTS (SELECT 1 FROM Users WHERE LOWER(Email) = LOWER('staff.tuvan@clothingshop.local'))
        INSERT INTO Users (FullName, Email, PasswordHash, RoleId, IsActive)
        VALUES (N'Nhân viên Tư vấn', 'staff.tuvan@clothingshop.local', @DefaultHash, @StaffRoleId, 1);

    IF NOT EXISTS (SELECT 1 FROM Users WHERE LOWER(Email) = LOWER('staff.cskh@clothingshop.local'))
        INSERT INTO Users (FullName, Email, PasswordHash, RoleId, IsActive)
        VALUES (N'Nhân viên CSKH', 'staff.cskh@clothingshop.local', @DefaultHash, @StaffRoleId, 1);
END

-- 2. Them cac tai khoan KHACH HANG (CUSTOMER)
IF @CustomerRoleId IS NOT NULL
BEGIN
    IF NOT EXISTS (SELECT 1 FROM Users WHERE LOWER(Email) = LOWER('khach1.test@clothingshop.local'))
        INSERT INTO Users (FullName, Email, PasswordHash, RoleId, IsActive)
        VALUES (N'Khách hàng Test 01', 'khach1.test@clothingshop.local', @DefaultHash, @CustomerRoleId, 1);

    IF NOT EXISTS (SELECT 1 FROM Users WHERE LOWER(Email) = LOWER('khach2.test@clothingshop.local'))
        INSERT INTO Users (FullName, Email, PasswordHash, RoleId, IsActive)
        VALUES (N'Khách hàng Test 02', 'khach2.test@clothingshop.local', @DefaultHash, @CustomerRoleId, 1);

    IF NOT EXISTS (SELECT 1 FROM Users WHERE LOWER(Email) = LOWER('khach3.test@clothingshop.local'))
        INSERT INTO Users (FullName, Email, PasswordHash, RoleId, IsActive)
        VALUES (N'Khách hàng Test 03', 'khach3.test@clothingshop.local', @DefaultHash, @CustomerRoleId, 1);
END

SELECT u.UserId, u.FullName, u.Email, r.RoleName, u.IsActive
FROM Users u
LEFT JOIN Roles r ON u.RoleId = r.RoleId
WHERE u.Email LIKE '%@clothingshop.local'
ORDER BY r.RoleName, u.UserId;

GO

-- >>> BO SUNG 2 TAI KHOAN STAFF TEST CHAT NOI BO (Mat khau mac dinh: AdminShop123@) <<<
DECLARE @SampleHash NVARCHAR(255);
DECLARE @StaffRoleId INT;

SELECT TOP 1 @SampleHash = PasswordHash
FROM dbo.Users
WHERE Email IN ('staff@clothingshop.local', 'admin@clothingshop.local')
ORDER BY CASE WHEN Email = 'staff@clothingshop.local' THEN 0 ELSE 1 END;

SELECT TOP 1 @StaffRoleId = RoleId FROM dbo.Roles WHERE RoleName = 'STAFF';

IF @SampleHash IS NOT NULL AND @StaffRoleId IS NOT NULL
BEGIN
    IF NOT EXISTS (SELECT 1 FROM dbo.Users WHERE Email = 'staff2@clothingshop.local')
    BEGIN
        INSERT INTO dbo.Users (Email, PasswordHash, FullName, Phone, RoleId, IsActive)
        VALUES ('staff2@clothingshop.local', @SampleHash, N'Nhân viên Bán hàng 2', '0901000002', @StaffRoleId, 1);
    END

    IF NOT EXISTS (SELECT 1 FROM dbo.Users WHERE Email = 'staff3@clothingshop.local')
    BEGIN
        INSERT INTO dbo.Users (Email, PasswordHash, FullName, Phone, RoleId, IsActive)
        VALUES ('staff3@clothingshop.local', @SampleHash, N'Nhân viên Kho 3', '0901000003', @StaffRoleId, 1);
    END
END
GO
