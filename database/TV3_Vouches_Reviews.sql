USE ClothingShopDB;
GO

IF OBJECT_ID('dbo.Vouchers', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.Vouchers (
        VoucherId INT IDENTITY(1,1) PRIMARY KEY,
        Code VARCHAR(50) NOT NULL UNIQUE,
        Description NVARCHAR(255) NULL,
        DiscountType VARCHAR(20) NOT NULL CHECK (DiscountType IN ('PERCENT', 'FIXED')),
        DiscountValue DECIMAL(18,2) NOT NULL CHECK (DiscountValue > 0),
        MinOrderAmount DECIMAL(18,2) NOT NULL DEFAULT 0 CHECK (MinOrderAmount >= 0),
        MaxDiscountAmount DECIMAL(18,2) NULL,
        Quantity INT NOT NULL DEFAULT 0 CHECK (Quantity >= 0),
        StartDate DATETIME2(0) NOT NULL,
        EndDate DATETIME2(0) NOT NULL,
        IsActive BIT NOT NULL DEFAULT 1
    );

    INSERT INTO dbo.Vouchers (Code, Description, DiscountType, DiscountValue, MinOrderAmount, MaxDiscountAmount, Quantity, StartDate, EndDate, IsActive)
    VALUES
    ('WELCOME10', N'Giảm 10% tối đa 50.000đ cho đơn từ 200.000đ', 'PERCENT', 10, 200000, 50000, 100, DATEADD(DAY, -1, SYSDATETIME()), DATEADD(DAY, 30, SYSDATETIME()), 1),
    ('GIAM30K', N'Giảm trực tiếp 30.000đ cho đơn từ 300.000đ', 'FIXED', 30000, 300000, NULL, 50, DATEADD(DAY, -1, SYSDATETIME()), DATEADD(DAY, 30, SYSDATETIME()), 1);
END;
GO

IF OBJECT_ID('dbo.ProductReviews', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.ProductReviews (
        ReviewId INT IDENTITY(1,1) PRIMARY KEY,
        ProductId INT NOT NULL,
        UserId INT NOT NULL,
        Rating INT NOT NULL CHECK (Rating BETWEEN 1 AND 5),
        Comment NVARCHAR(1000) NULL,
        CreatedAt DATETIME2(0) NOT NULL DEFAULT SYSDATETIME(),
        CONSTRAINT FK_ProductReviews_Products FOREIGN KEY (ProductId) REFERENCES dbo.Products(ProductId),
        CONSTRAINT FK_ProductReviews_Users FOREIGN KEY (UserId) REFERENCES dbo.Users(UserId)
    );
END;
GO