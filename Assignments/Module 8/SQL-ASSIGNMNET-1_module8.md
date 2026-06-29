# SQL Query Collection

## Query 1 — New Customers Acquired in June 2023

```sql
SELECT
    p.party_id,
    pp.first_name,
    pp.last_name,
    p.created_stamp AS entry_date,
    cm_email.info_string AS email,
    tn.contact_number AS phone
FROM party p

JOIN party_role pr
    ON p.party_id = pr.party_id

JOIN person pp
    ON pp.party_id = p.party_id

LEFT JOIN party_contact_mech pcm_email
    ON p.party_id = pcm_email.party_id

LEFT JOIN contact_mech cm_email
    ON pcm_email.contact_mech_id = cm_email.contact_mech_id
   AND cm_email.contact_mech_type_id = 'EMAIL_ADDRESS'

LEFT JOIN party_contact_mech pcm_phone
    ON p.party_id = pcm_phone.party_id

LEFT JOIN telecom_number tn
    ON pcm_phone.contact_mech_id = tn.contact_mech_id

WHERE pr.role_type_id = 'CUSTOMER'
  AND p.created_stamp >= '2025-06-01'
  AND p.created_stamp < '2026-07-01';
```

---

## Query 2 — List All Active Physical Products

```sql
SELECT
    product_id,
    product_type_id,
    internal_name
FROM product
WHERE product_type_id = 'FINISHED_GOOD'
  AND (
        sales_discontinuation_date IS NULL
        OR sales_discontinuation_date > CURRENT_TIMESTAMP
      );
```

---

## Query 3 — Products Missing NetSuite ID

```sql
SELECT
    p.product_id,
    p.internal_name,
    p.product_type_id,
    gi.id_value AS netsuite_id
FROM product p

LEFT JOIN good_identification gi
       ON p.product_id = gi.product_id
      AND gi.good_identification_type_id = 'ERP_ID'

WHERE gi.id_value IS NULL;
```

---

## Query 4 — Product IDs Across Systems

```sql
SELECT
    p.product_id,
    gi_erp.id_value AS ns_id,
    gi_hs.id_value AS hs_id,
    gi_shop.id_value AS shopify_id
FROM product p

LEFT JOIN good_identification gi_erp
    ON p.product_id = gi_erp.product_id
   AND gi_erp.good_identification_type_id = 'ERP_ID'

LEFT JOIN good_identification gi_hs
    ON p.product_id = gi_hs.product_id
   AND gi_hs.good_identification_type_id = 'HS_CODE'

LEFT JOIN good_identification gi_shop
    ON p.product_id = gi_shop.product_id
   AND gi_shop.good_identification_type_id = 'SHOPIFY_PROD_ID';
```

---

## Query 5 — Completed Orders in August 2023

```sql
SELECT
    oi.product_id,
    p.product_type_id,
    oh.product_store_id,
    oi.quantity AS total_quantity,
    p.internal_name,
    f.facility_id,
    f.external_id,
    f.facility_type_id,
    os.order_status_id AS order_history_id,
    oh.order_id,
    oi.order_item_seq_id
FROM order_header oh

JOIN order_item oi
    ON oh.order_id = oi.order_id

JOIN product p
    ON oi.product_id = p.product_id

LEFT JOIN order_status os
    ON oh.order_id = os.order_id

LEFT JOIN order_item_ship_group oisg
    ON oh.order_id = oisg.order_id

LEFT JOIN facility f
    ON oisg.facility_id = f.facility_id

WHERE oh.status_id = 'ORDER_COMPLETED'
  AND oh.order_date >= '2023-08-01'
  AND oh.order_date < '2023-09-01';
```

---

## Query 6 — Newly Created Sales Orders and Payment Methods

```sql
SELECT
    opp.payment_method_type_id,
    opp.order_id,
    opp.max_amount,
    oh.external_id
FROM order_payment_preference opp

JOIN order_header oh
    ON oh.order_id = opp.order_id

WHERE oh.order_type_id = 'SALES_ORDER'
ORDER BY opp.order_id;
```

---

## Query 8 — Payment Captured but Not Shipped

```sql
SELECT DISTINCT
    oh.order_id,
    oh.status_id AS order_status,
    opp.status_id AS payment_status,
    s.status_id AS shipment_status
FROM order_header oh

JOIN order_payment_preference opp
    ON oh.order_id = opp.order_id

LEFT JOIN order_item_ship_group oisg
    ON oh.order_id = oisg.order_id

LEFT JOIN shipment s
    ON oisg.ship_group_seq_id = s.primary_ship_group_seq_id
   AND oisg.order_id = s.primary_order_id

WHERE opp.status_id IN (
        'PAYMENT_RECEIVED',
        'PAYMENT_SETTLED',
        'PAYMENT_CAPTURED'
      )
  AND (
        s.shipment_id IS NULL
        OR s.status_id <> 'SHIPMENT_SHIPPED'
      );
```

---

## Query 9 — Orders Completed Hourly

```sql
SELECT
    EXTRACT(HOUR FROM status_datetime) AS hour,
    COUNT(*)
FROM order_status

WHERE status_id = 'ORDER_COMPLETED'

GROUP BY EXTRACT(HOUR FROM status_datetime)

ORDER BY hour;
```

---

## Query 10 — BOPIS Orders Revenue (Last Year)

```sql
SELECT
    COUNT(DISTINCT oh.order_id) AS total_orders,
    SUM(oh.grand_total) AS total_revenue
FROM order_header oh

JOIN order_item_ship_group oisg
    ON oh.order_id = oisg.order_id

JOIN shipment s
    ON s.primary_order_id = oh.order_id

WHERE oisg.shipment_method_type_id = 'STOREPICKUP'
  AND s.status_id = 'SHIPMENT_SHIPPED'
  AND s.estimated_ship_date >= '2025-01-01'
  AND s.estimated_ship_date < '2026-01-01';
```

---

## Query 11 — Canceled Orders (Last Month)

```sql
SELECT
    COUNT(order_id) AS cancelled_orders_count,
    change_reason
FROM order_status

WHERE status_id = 'ITEM_CANCELLED'
  AND status_datetime BETWEEN '2026-05-01' AND '2026-06-01'

GROUP BY change_reason;
```

---

## Query 12 — Product Threshold Value

```sql
SELECT
    product_id,
    SUM(minimum_stock) AS total_minimum_stock
FROM product_facility

GROUP BY product_id;
```
