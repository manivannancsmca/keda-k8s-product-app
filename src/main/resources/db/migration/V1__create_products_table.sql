CREATE TABLE products
(
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(200) NOT NULL,
    description VARCHAR(2000),
    price DECIMAL(19,4) NOT NULL,
    stock_quantity INT NOT NULL,
    category VARCHAR(50) NOT NULL,
    image_url VARCHAR(500),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),

    INDEX idx_category (category),
    INDEX idx_name (name)
);