## Query 13 — Shipping Addresses for October 2023 Orders

```sql
SELECT
    oh.order_id,
    p.party_id,
    p.first_name,
    p.last_name,
    pa.address1,
    pa.city,
    pa.state_province_geo_id,
    pa.country_geo_id,
    oh.status_id,
    oh.order_date
FROM order_header oh

LEFT JOIN order_role orr
    ON oh.order_id = orr.order_id
    AND orr.role_type_id = 'SHIP_TO_CUSTOMER'

LEFT JOIN order_contact_mech ocm
    ON oh.order_id = ocm.order_id
    AND ocm.contact_mech_purpose_type_id = 'SHIPPING_LOCATION'

LEFT JOIN postal_address pa
    ON pa.contact_mech_id = ocm.contact_mech_id

LEFT JOIN person p
    ON p.party_id = orr.party_id

WHERE (oh.order_date >= '2023-10-01'
       AND oh.order_date < '2023-11-01')
   OR (oh.status_id = 'ORDER_COMPLETED'
       AND oh.last_updated_stamp >= '2023-10-01'
       AND oh.last_updated_stamp < '2023-11-01');
```

---

## Query 14 — Orders from New York

```sql
SELECT
    oh.order_id,
    p.first_name,
    p.last_name,
    oh.grand_total,
    pa.address1,
    pa.city,
    pa.state_province_geo_id,
    pa.country_geo_id,
    oh.status_id,
    oh.order_date
FROM order_header oh

LEFT JOIN order_role orr
    ON oh.order_id = orr.order_id
    AND orr.role_type_id = 'SHIP_TO_CUSTOMER'

LEFT JOIN order_contact_mech ocm
    ON oh.order_id = ocm.order_id
    AND ocm.contact_mech_purpose_type_id = 'SHIPPING_LOCATION'

LEFT JOIN postal_address pa
    ON pa.contact_mech_id = ocm.contact_mech_id

LEFT JOIN person p
    ON p.party_id = orr.party_id

WHERE oh.status_id = 'ORDER_COMPLETED'
  AND pa.state_province_geo_id = 'NY';
```

---

## Query 15 — Top-Selling Product in New York

```sql
SELECT
    oi.product_id,
    p.internal_name,
    pa.state_province_geo_id,
    SUM(oi.quantity)                    AS total_quantity_sold,
    SUM(oi.quantity * oi.unit_price)    AS revenue
FROM order_header oh

JOIN order_item oi
    ON oh.order_id = oi.order_id

JOIN product p
    ON p.product_id = oi.product_id

LEFT JOIN order_contact_mech ocm
    ON oh.order_id = ocm.order_id
   AND ocm.contact_mech_purpose_type_id = 'SHIPPING_LOCATION'

JOIN postal_address pa
    ON pa.contact_mech_id = ocm.contact_mech_id
   AND pa.state_province_geo_id = 'NY'

WHERE oh.status_id = 'ORDER_COMPLETED'

GROUP BY
    oi.product_id,
    p.internal_name,
    pa.state_province_geo_id

ORDER BY total_quantity_sold DESC

LIMIT 1;
```

---

## Query 16 — Store-Specific (Facility-Wise) Revenue

```sql
SELECT
    f.facility_id,
    f.facility_name,
    COUNT(DISTINCT oi.order_id) AS total_orders,
    SUM(oi.quantity * oi.unit_price) AS total_revenue,
    MIN(oi.created_stamp) AS start_date,
    MAX(oi.created_stamp) AS end_date
FROM order_item oi

JOIN product_facility pf
    ON pf.product_id = oi.product_id

JOIN facility f
    ON f.facility_id = pf.facility_id

GROUP BY
    f.facility_id,
    f.facility_name;
```

---

## Query 17 — Lost and Damaged Inventory

```sql
SELECT
    ii.product_id,
    ii.facility_id,
    SUM(iid.quantity_on_hand_diff) AS total_diff,
    iid.reason_enum_id
FROM inventory_item_detail iid

LEFT JOIN inventory_item ii
    ON ii.inventory_item_id = iid.inventory_item_id

WHERE iid.reason_enum_id IN ('VAR_DAMAGED', 'VAR_LOST')

GROUP BY
    ii.product_id,
    ii.facility_id,
    iid.reason_enum_id;
```

---

## Query 18 — Low Stock or Out of Stock Items Report

```sql
SELECT
    pf.product_id,
    p.product_name,
    pf.facility_id,
    ii.quantity_on_hand_total AS qoh,
    ii.available_to_promise AS atp,
    pf.minimum_stock AS reorder_threshold
FROM product_facility pf

JOIN product p
    ON pf.product_id = p.product_id

LEFT JOIN inventory_item ii
    ON ii.product_id = pf.product_id
   AND ii.facility_id = pf.facility_id

WHERE ii.available_to_promise < pf.minimum_stock
   OR ii.available_to_promise <= 0;
```

---

## Query 19 — Retrieve the Current Facility (Physical or Virtual) of Open Orders

```sql
SELECT DISTINCT
    f.facility_name,
    f.facility_type_id,
    f.facility_id,
    oh.status_id AS order_status,
    oh.order_id
FROM order_item oi

 JOIN order_header oh
    ON oh.order_id = oi.order_id

LEFT JOIN order_item_ship_group oisg
    ON oisg.order_id = oi.order_id
   AND oisg.ship_group_seq_id = oi.ship_group_seq_id

LEFT JOIN facility f
    ON oisg.facility_id = f.facility_id

WHERE oh.status_id NOT IN (
    'ORDER_COMPLETED',
    'ORDER_CANCELLED'
);
```

---

## Query 20 — Items Where QOH and ATP Differ

```sql
SELECT
    product_id,
    facility_id,
    quantity_on_hand_total AS qoh,
    available_to_promise_total AS atp,
    (quantity_on_hand_total - available_to_promise_total) AS difference
FROM inventory_item;
```

---

## Query 21 — Order Item Current Status Changed Date-Time

```sql
SELECT
    os1.order_id,
    os2.order_item_seq_id,
    os2.status_id AS current_status_id,,
    os2.status_user_login AS status_change_datetime,
    os2.status_datetime AS changed_by
FROM order_status os1

JOIN order_status os2
    ON os1.order_id = os2.order_id
   AND os1.order_item_seq_id = os2.order_item_seq_id

WHERE os1.status_id = 'ITEM_APPROVED'
  AND os2.status_id = 'ITEM_COMPLETED';
```

---

## Query 22 — Total Orders by Sales Channel

```sql
SELECT
    oh.sales_channel_enum_id            AS sales_channel,
    COUNT(oh.order_id)                  AS total_orders,
    SUM(oh.grand_total)                 AS total_revenue,
    MIN(oh.order_date)                  AS start_date,
    MAX(oh.order_date)                  AS end_date
FROM order_header oh

WHERE oh.status_id = 'ORDER_COMPLETED'

GROUP BY oh.sales_channel_enum_id

ORDER BY total_orders DESC;
```
