USE ClothingShopDB;
GO

IF OBJECT_ID(N'dbo.StockTransactions', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.StockTransactions (
        TransactionId BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        VariantId INT NOT NULL,
        TransactionType VARCHAR(20) NOT NULL,
        QuantityChange INT NOT NULL,
        StockBefore INT NOT NULL,
        StockAfter INT NOT NULL,
        ReferenceCode VARCHAR(50) NULL,
        Note NVARCHAR(500) NULL,
        CreatedBy INT NOT NULL,
        CreatedAt DATETIME2(0) NOT NULL CONSTRAINT DF_StockTransactions_CreatedAt DEFAULT SYSDATETIME(),
        CONSTRAINT FK_StockTransactions_ProductVariants FOREIGN KEY (VariantId) REFERENCES dbo.ProductVariants(VariantId),
        CONSTRAINT FK_StockTransactions_Users FOREIGN KEY (CreatedBy) REFERENCES dbo.Users(UserId),
        CONSTRAINT CK_StockTransactions_Type CHECK (TransactionType IN ('IMPORT','SALE','RETURN','ADJUSTMENT')),
        CONSTRAINT CK_StockTransactions_Quantity CHECK (QuantityChange <> 0),
        CONSTRAINT CK_StockTransactions_Stock CHECK (StockBefore >= 0 AND StockAfter >= 0)
    );
    CREATE INDEX IX_StockTransactions_CreatedAt ON dbo.StockTransactions(CreatedAt DESC);
    CREATE INDEX IX_StockTransactions_VariantId ON dbo.StockTransactions(VariantId, CreatedAt DESC);
END;
GO

DECLARE @statusConstraint sysname;
SELECT TOP 1 @statusConstraint = cc.name
FROM sys.check_constraints cc
JOIN sys.tables t ON t.object_id = cc.parent_object_id
WHERE t.name = 'Orders' AND cc.definition LIKE '%PENDING%CONFIRMED%PROCESSING%SHIPPING%COMPLETED%CANCELLED%';
IF @statusConstraint IS NOT NULL
    EXEC('ALTER TABLE dbo.Orders DROP CONSTRAINT ' + QUOTENAME(@statusConstraint));
ALTER TABLE dbo.Orders WITH CHECK ADD CONSTRAINT CK_Orders_OrderStatus_TV4
CHECK (OrderStatus IN ('PENDING','CONFIRMED','PROCESSING','SHIPPING','COMPLETED','CANCELLED','RETURN_REQUESTED','RETURN_RECEIVED','RETURNED'));
GO
