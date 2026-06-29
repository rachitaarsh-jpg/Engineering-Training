## Query 23 — Completed Sales Orders (Physical Items)

```sql
SELECT
    oi.order_id,
    oi.order_item_seq_id,
    p.product_id,
    p.product_type_id,
    oh.sales_channel_enum_id,
    oh.order_date,
    oh.entry_date,
    oh.status_id,
    oh.order_type_id,
    oh.product_store_id
FROM order_header oh
JOIN order_item oi
    ON oh.order_id = oi.order_id
LEFT JOIN product p
    ON p.product_id = oi.product_id
JOIN product_type pt
    ON p.product_type_id = pt.product_type_id
WHERE pt.is_physical = 'Y'
  AND oi.status_id <> 'ITEM_CANCELLED'
ORDER BY oi.order_id ASC;
```

---

## Query 24 — Completed Return Items

```sql
SELECT
    p.party_id,
    ri.order_id,
    p.first_name,
    p.last_name
FROM return_header rh
JOIN return_item ri
    ON ri.return_id = rh.return_id
LEFT JOIN person p
    ON p.party_id = rh.from_party_id
WHERE ri.order_id IN (
    SELECT order_id
    FROM return_item
    GROUP BY order_id
    HAVING COUNT(return_item_id) = 1
)
AND rh.return_date BETWEEN '2026-05-01' AND '2026-06-01';
```

---

## Query 25 — Single-Return Orders (Last Month)

```sql
SELECT
    p.party_id,
    ri.order_id,
    p.first_name,
    p.last_name
FROM return_header rh
JOIN return_item ri
    ON ri.return_id = rh.return_id
LEFT JOIN person p
    ON p.party_id = rh.from_party_id
WHERE ri.order_id IN (
    SELECT order_id
    FROM return_item
    GROUP BY order_id
    HAVING COUNT(return_item_id) = 1
);
```

---

## Query 26 — Returns and Appeasements

```sql
SELECT
    COUNT(ri.return_item_id) AS total_returned_items,
    SUM(
        CASE
            WHEN ra.return_adjustment_type_id = 'Appeasement'
            THEN 1
            ELSE 0
        END
    ) AS appeasement_count
FROM return_item ri
LEFT JOIN return_adjustment ra
    ON ri.return_id = ra.return_id;
```

---

## Query 27 — Detailed Return Information

```sql
SELECT
    rh.return_id,
    oh.entry_date,
    ra.return_adjustment_type_id,
    ra.amount,
    ra.comments,
    oh.order_id,
    oh.order_date,
    rh.return_date,
    oh.product_store_id
FROM return_header rh
JOIN return_item ri
    ON rh.return_id = ri.return_id
LEFT JOIN order_header oh
    ON ri.order_id = oh.order_id
LEFT JOIN return_adjustment ra
    ON ra.return_id = ri.return_id;
```

---

## Query 28 — Orders with Multiple Returns

```sql
SELECT DISTINCT
    ri.order_id,
    ri.return_id,
    rh.return_date,
    ri.return_reason_id,
    ri.return_quantity
FROM return_item ri
JOIN (
    SELECT order_id
    FROM return_item
    GROUP BY order_id
    HAVING COUNT(DISTINCT return_id) >= 2
) fo
    ON fo.order_id = ri.order_id
LEFT JOIN return_header rh
    ON rh.return_id = ri.return_id;
```

---

## Query 29 — Store with Most One-Day Shipped Orders (Last Month)

```sql
SELECT
    ois.facility_id,
    f.facility_name,
    COUNT(oi.order_id) AS total_order_items
FROM order_item oi
JOIN order_item_ship_group ois
    ON ois.order_id = oi.order_id
   AND ois.ship_group_seq_id = oi.ship_group_seq_id
LEFT JOIN facility f
    ON ois.facility_id = f.facility_id
JOIN order_status os
    ON os.order_id = oi.order_id
   AND os.status_id = 'ITEM_COMPLETED'
WHERE ois.shipment_method_type_id = 'NEXT_DAY'
  AND f.facility_type_id NOT IN (
      SELECT facility_type_id
      FROM facility_type
      WHERE parent_type_id = 'VIRTUAL_FACILITY'
  )
  AND oi.status_id = 'ITEM_COMPLETED'
  AND os.status_datetime >= NOW() - INTERVAL 30 DAY
GROUP BY
    ois.facility_id,
    f.facility_name;
```

---

## Query 30 — List of Warehouse Pickers

```sql
SELECT
    p.party_id,
    per.first_name,
    per.last_name,
    p.status_id,
    fp.facility_id
FROM party p
LEFT JOIN person per
    ON p.party_id = per.party_id
JOIN party_role pr
    ON p.party_id = pr.party_id
   AND pr.role_type_id = 'WAREHOUSE_PICKER'
LEFT JOIN facility_party fp
    ON fp.party_id = p.party_id
   AND fp.role_type_id = 'WAREHOUSE_PICKER';
```

---

## Query 31 — Total Facilities That Sell the Product

```sql
SELECT
    p.product_id,
    p.internal_name,
    COUNT(DISTINCT pf.facility_id) AS facility_count
FROM product p
JOIN product_price pp
    ON pp.product_id = p.product_id
   AND pp.product_price_type_id = 'LIST_PRICE'
LEFT JOIN product_facility pf
    ON pf.product_id = p.product_id
GROUP BY
    p.product_id,
    p.internal_name;
```

---

## Query 32 — Total Items in Various Virtual Facilities

```sql
SELECT
    pf.product_id,
    pf.facility_id,
    f.facility_type_id,
    ii.quantity_on_hand_total AS qoh,
    ii.available_to_promise_total AS atp
FROM product_facility pf
LEFT JOIN inventory_item ii
    ON ii.product_id = pf.product_id
   AND ii.facility_id = pf.facility_id
JOIN facility f
    ON f.facility_id = pf.facility_id
WHERE f.facility_type_id NOT IN (
    SELECT facility_type_id
    FROM facility_type
    WHERE parent_type_id = 'VIRTUAL_FACILITY'
);
```

---

## Query 33 — Transfer Orders Without Inventory Reservation

```sql
SELECT
    oh.order_id AS transfer_order_id,
    oh.origin_facility_id AS from_facility_id,
    ois.facility_id AS to_facility_id,
    oi.product_id,
    oi.quantity AS requested_quantity,
    0 AS reserved_quantity,
    oh.order_date AS transfer_date,
    oh.status_id AS status
FROM order_header oh
JOIN order_item oi
    ON oi.order_id = oh.order_id
JOIN order_item_ship_group ois
    ON ois.order_id = oh.order_id
LEFT JOIN order_item_ship_grp_inv_res otshir
    ON otshir.order_id = ois.order_id
   AND otshir.ship_group_seq_id = ois.ship_group_seq_id
   AND otshir.order_item_seq_id = oi.order_item_seq_id
WHERE oh.order_type_id = 'TRANSFER_ORDER'
  AND otshir.reserved_datetime IS NULL
  AND oh.status_id = 'ORDER_APPROVED'
ORDER BY oh.order_id;
```

---

## Query 34 — Orders Without Picklist

```sql
SELECT DISTINCT
    oh.order_id,
    oh.order_date,
    oh.status_id AS order_status,
    ois.facility_id
FROM order_header oh
JOIN order_item_ship_group ois
    ON oh.order_id = ois.order_id
LEFT JOIN shipment s
    ON ois.order_id = s.primary_order_id
LEFT JOIN facility f
    ON f.facility_id = ois.facility_id
WHERE s.shipment_id IS NULL
  AND oh.status_id = 'ORDER_APPROVED'
  AND f.facility_type_id NOT IN (
      SELECT facility_type_id
      FROM facility_type
      WHERE parent_type_id = 'VIRTUAL_FACILITY'
  );
```
