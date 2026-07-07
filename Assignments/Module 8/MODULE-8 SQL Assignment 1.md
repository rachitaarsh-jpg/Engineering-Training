# MODULE-8 SQL Assignment 1

---

## 1. New Customers Acquired in June 2023

### Business Problem
The marketing team ran a campaign in June 2023 and wants to see how many new customers signed up during that period.

```sql
SELECT
    p.PARTY_ID,
    p.FIRST_NAME,
    p.LAST_NAME,
    MAX(cm_email.INFO_STRING) AS EMAIL,
    MAX(tn.CONTACT_NUMBER) AS PHONE,
    p.CREATED_STAMP AS ENTRY_DATE
    
FROM person p

LEFT JOIN party_contact_mech pcm_email
    ON p.PARTY_ID = pcm_email.PARTY_ID 
LEFT JOIN contact_mech cm_email
    ON pcm_email.CONTACT_MECH_ID = cm_email.CONTACT_MECH_ID
    AND cm_email.CONTACT_MECH_TYPE_ID='EMAIL_ADDRESS'
    
LEFT JOIN party_contact_mech pcm_tn
    ON p.PARTY_ID = pcm_tn.PARTY_ID
LEFT JOIN contact_mech cm_tele 
    ON pcm_tn.CONTACT_MECH_ID = cm_tele.CONTACT_MECH_ID
    AND cm_tele.CONTACT_MECH_TYPE_ID='TELECOM_NUMBER'
LEFT JOIN telecom_number tn 
    ON cm_tele.CONTACT_MECH_ID = tn.CONTACT_MECH_ID
    
WHERE DATE(p.CREATED_STAMP) > '2026-05-31'
  AND DATE(p.CREATED_STAMP) < '2026-07-01'
  
  GROUP BY  p.PARTY_ID,
            p.FIRST_NAME,
            p.LAST_NAME,
            p.CREATED_STAMP;
```

---

## 2. List All Active Physical Products

### Business Problem
Merchandising teams often need a list of all physical products to manage logistics, warehousing, and shipping.

```sql
SELECT

    PRODUCT_ID,
    PRODUCT_TYPE_ID,
    INTERNAL_NAME,
  
FROM product 

WHERE IS_VARIANT = 'Y' AND IS_VIRTUAL = 'N'
AND PRODUCT_TYPE_ID = 'FINISHED_GOOD'
AND SALES_DISCONTINUATION_DATE > CURDATE();
```

---

## 3. Products Missing NetSuite ID

### Business Problem
A product cannot sync to NetSuite unless it has a valid NetSuite ID. The OMS needs a list of all products that still need to be created or updated in NetSuite.

```sql
SELECT
    p.PRODUCT_ID,
    p.PRODUCT_TYPE_ID,
    p.INTERNAL_NAME,
    gi.GOOD_IDENTIFICATION_TYPE_ID
FROM product p
JOIN good_identification gi
    ON p.PRODUCT_ID = gi.PRODUCT_ID
WHERE gi.GOOD_IDENTIFICATION_TYPE_ID = 'NETSUITE_PRODUCT_ID'
    AND (gi.ID_VALUE IS NULL OR gi.ID_VALUE = ' ');
```

---

## 4. Product IDs Across Systems

### Business Problem
To sync an order or product across multiple systems (e.g., Shopify, HotWax, ERP/NetSuite), the OMS needs to know each system's unique identifier for that product. This query retrieves the Shopify ID, HotWax ID, and ERP ID (NetSuite ID) for all products.

```sql
SELECT
    p.PRODUCT_ID,
    oi.EXTERNAL_ID AS SHOPIFY_ID,
    oi.ORDER_ID AS HOTWAX_ID,
    gi.ID_VALUE AS NETSUITE_ID
FROM order_item oi
JOIN product p
    ON oi.PRODUCT_ID = p.PRODUCT_ID
JOIN good_identification gi
    ON p.PRODUCT_ID = gi.PRODUCT_ID
WHERE gi.GOOD_IDENTIFICATION_TYPE_ID IN ('ERP_ID', 'NETSUITE_PRODUCT_ID');
```

---

## 5. Completed Orders in August 2023

### Business Problem
After running similar reports for a previous month, you now need all completed orders in August 2023 for analysis.

```sql
SELECT
    oi.PRODUCT_ID,
    p.PRODUCT_TYPE_ID,
    oh.PRODUCT_STORE_ID,
    oi.QUANTITY AS TOTAL_QUANTITY,
    p.INTERNAL_NAME,
    oisg.FACILITY_ID,
    oh.EXTERNAL_ID,
    f.FACILITY_TYPE_ID,
    os.ORDER_STATUS_ID AS ORDER_HISTORY,
    oh.ORDER_ID,
    oi.ORDER_ITEM_SEQ_ID,
    oisg.SHIP_GROUP_SEQ_ID
FROM order_header oh
JOIN order_item oi
    ON oh.ORDER_ID = oi.ORDER_ID
JOIN order_item_ship_group oisg
    ON oi.ORDER_ID = oisg.ORDER_ID
   AND oi.SHIP_GROUP_SEQ_ID = oisg.SHIP_GROUP_SEQ_ID
JOIN product p
    ON oi.PRODUCT_ID = p.PRODUCT_ID
LEFT JOIN facility f
    ON oisg.FACILITY_ID = f.FACILITY_ID
LEFT JOIN order_status os
    ON oh.ORDER_ID = os.ORDER_ID
   AND os.STATUS_ID = 'ORDER_COMPLETED'
WHERE os.STATUS_ID = 'ORDER_COMPLETED'
  AND oh.ORDER_DATE >= '2023-08-01 00:00:00'
  AND oh.ORDER_DATE < '2023-09-01 00:00:00';
```

---

## 7. Newly Created Sales Orders and Payment Methods

### Business Problem
Finance teams need to see new orders and their payment methods for reconciliation and fraud checks.

```sql
SELECT
    oh.ORDER_ID,
    opp.MAX_AMOUNT,
    opp.PAYMENT_METHOD_TYPE_ID,
    oh.EXTERNAL_ID AS "SHOPIFY ORDER ID"
FROM order_header oh
JOIN order_payment_preference opp
    ON oh.ORDER_ID = opp.ORDER_ID;
```

---

## 8. Payment Captured but Not Shipped

### Business Problem
Finance teams want to ensure revenue is recognized properly. If payment is captured but no shipment has occurred, it warrants further review.

```sql
SELECT
    oh.ORDER_ID,
    os.STATUS_ID,
    opp.STATUS_ID,
    ss.STATUS_ID AS "SHIPMENT STATUS"
FROM order_header oh
JOIN order_payment_preference opp
    ON oh.ORDER_ID = opp.ORDER_ID
JOIN order_status os
    ON opp.ORDER_PAYMENT_PREFERENCE_ID = os.ORDER_PAYMENT_PREFERENCE_ID
JOIN order_shipment ors
    ON oh.ORDER_ID = ors.ORDER_ID
JOIN shipment_status ss
    ON ors.SHIPMENT_ID = ss.SHIPMENT_ID
WHERE os.STATUS_ID IN ('PAYMENT_RECEIVED', 'PAYMENT_SETTLED')
  AND ss.STATUS_ID IN (
        'SHIPMENT_INPUT',
        'SHIPMENT_PACKED',
        'SHIPMENT_PICKED',
        'SHIPMENT_SCHEDULED'
      );
```

---

## 9. Orders Completed Hourly

### Business Problem
Operations teams may want to see how orders complete across the day to schedule staffing.

```sql
SELECT
    COUNT(DISTINCT oh.ORDER_ID) AS `TOTAL ORDERS`,
    HOUR(os.STATUS_DATETIME) AS `HOUR`
FROM order_header oh
JOIN order_status os
    ON oh.ORDER_ID = os.ORDER_ID
WHERE oh.STATUS_ID = 'ORDER_COMPLETED'
  AND os.STATUS_ID = 'ORDER_COMPLETED'
GROUP BY HOUR(os.STATUS_DATETIME)
ORDER BY `HOUR` ASC;
```

---

## 10. BOPIS Orders Revenue (Last Year)

### Business Problem
BOPIS (Buy Online, Pickup In Store) is a key retail strategy. Finance wants to know the revenue from BOPIS orders for the previous year.

```sql
SELECT
    COUNT(oh.ORDER_ID) AS `TOTAL ORDERS`,
    SUM(oh.GRAND_TOTAL) AS `TOTAL REVENUE`
FROM order_header oh
WHERE oh.STATUS_ID = 'ORDER_COMPLETED'
  AND YEAR(oh.ORDER_DATE) = YEAR(CURDATE()) - 1
  AND EXISTS (
        SELECT 1
        FROM order_item_ship_group oisg
        WHERE oisg.ORDER_ID = oh.ORDER_ID
          AND oisg.SHIPMENT_METHOD_TYPE_ID = 'STOREPICKUP'
    );
```

---

## 11. Canceled Orders (Last Month)

### Business Problem
The merchandising team needs to know how many orders were canceled in the previous month and their reasons.

```sql
SELECT
    COUNT(DISTINCT oh.ORDER_ID) AS `TOTAL ORDERS`,
    os.CHANGE_REASON AS `CANCELLATION REASON`
FROM order_header oh
JOIN order_status os
    ON oh.ORDER_ID = os.ORDER_ID
WHERE oh.STATUS_ID = 'ORDER_CANCELLED'
  AND os.STATUS_ID = 'ORDER_CANCELLED'
  AND MONTH(os.STATUS_DATETIME) = MONTH(CURRENT_DATE - INTERVAL 1 MONTH)
GROUP BY os.CHANGE_REASON
ORDER BY `TOTAL ORDERS` DESC;
```

---

## 12. Product Threshold Value

### Business Problem
The retailer has set a threshold value for products that are sold online in order to avoid overselling.

```sql
SELECT
    PRODUCT_ID,
    MINIMUM_STOCK AS `THRESHOLD`
FROM PRODUCT_FACILITY
WHERE MINIMUM_STOCK IS NOT NULL;
```
