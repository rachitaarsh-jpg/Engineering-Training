# SQL Assignment 3

## 1 Completed Sales Orders (Physical Items)

### Business Problem  
Merchants need to track only physical items (requiring shipping and fulfillment) for logistics and shipping-cost analysis.

```sql
SELECT oh.order_id,  
       oi.order_item_seq_id,  
       p.product_id,  
       p.product_type_id,  
       oh.sales_channel_enum_id,  
       oh.order_date,  
       oh.entry_date,  
       os.status_id,  
       os.status_datetime,  
       oh.order_type_id,  
       oh.product_store_id  
         
       FROM ORDER_HEADER oh   
       JOIN ORDER_ITEM oi   
       ON oh.order_id = oi.order_id  
       JOIN ORDER_STATUS os   
       ON oh.order_id = os.order_id AND oi.order_item_seq_id = os.order_item_seq_id  
       JOIN PRODUCT p   
       ON oi.product_id = p.product_id  
         
       WHERE p.is_variant = 'Y' AND p.is_virtual = 'N'  
        AND oh.order_type_id = 'SALES_ORDER'  
        AND os.status_id = 'ITEM_COMPLETED';

```



---

## 2. Completed Return Items

### Business Problem  
Customer service and finance often need insights into returned items to manage refunds, replacements, and inventory restocking

```sql
SELECT rh.return_id,  
       ri.order_id,         
       oh.product_store_id,  
       rs.status_datetime,  
       oh.order_name,  
       rh.from_party_id,  
       rh.return_date,  
       rh.entry_date,  
       rh.return_channel_enum_id  
         
       FROM return_header rh   
       JOIN return_item ri   
       ON rh.return_id = ri.return_id  
       JOIN order_header oh  
       ON ri.order_id = oh.order_id  
       JOIN return_status rs   
       ON rh.return_id = rs.return_id  
         
       WHERE rs.status_id = 'RETURN_COMPLETED';

```



---

## 3 Single-Return Orders (Last Month)

### Business Problem  
The mechandising team needs a list of orders that only have one return.

```sql
SELECT p.party_id,  
       pr.first_name  
         
       FROM party p   
       JOIN person pr   
       ON p.party_id = pr.party_id  
       JOIN return_header rh  
       ON p.party_id = rh.from_party_id  
       JOIN return_item ri   
       ON rh.return_id = ri.return_id  
       JOIN order_header oh   
       ON ri.order_id = oh.order_id  
         
       WHERE oh.order_id IN (  
       SELECT order_id from return_item  
       GROUP BY order_id   
       HAVING COUNT(*) = 1  
       )  
         
       GROUP BY p.party_id, pr.first_name  
       ORDER BY pr.first_name;

```



---

## 4 Returns and Appeasements

### Business Problem  
The retailer needs the total amount of items, were returned as well as how many appeasements were issued.

```sql
SELECT  
    r.total_returns,  
    r.return_dollar_total,  
    a.total_appeasements,  
    a.appeasement_dollar_total  
FROM  
(  
    SELECT  
        COUNT(DISTINCT return_id) AS total_returns,  
        SUM(amount) AS return_dollar_total  
    FROM return_item_billing  
) r,  
(  
    SELECT  
        COUNT(*) AS total_appeasements,  
        SUM(amount) AS appeasement_dollar_total  
    FROM return_adjustment  
    WHERE return_adjustment_type_id = 'APPEASEMENT'  
) a;

```

---

## 5 Detailed Return Information

### Business Problem  
Certain teams need granular return data (reason, date, refund amount) for analyzing return rates, identifying recurring issues, or updating policies.

```sql
SELECT rh.return_id,  
       rh.entry_date,  
       ra.return_adjustment_type_id,  
       ri.return_reason_id,  
       rib.amount,  
       ra.comments,  
       oh.order_id,  
       oh.order_date,  
       rh.entry_date AS return_date,  
       oh.product_store_id
FROM return_header rh  
JOIN return_item ri   
  ON rh.return_id = ri.return_id  
JOIN return_item_billing rib  
  ON ri.return_id = rib.return_id   
 AND ri.return_item_seq_id = rib.return_item_seq_id  
JOIN order_header oh   
  ON ri.order_id = oh.order_id  
LEFT JOIN return_adjustment ra   
  ON rh.return_id = ra.return_id;

```

---

## 6 Orders with Multiple Returns

### Business Problem  
Analyzing orders with multiple returns can identify potential fraud, chronic issues with certain items, or inconsistent shipping processes

```sql
SELECT  
    ri.order_id,  
    rh.return_id,  
    rh.return_date,  
    ri.return_reason_id AS return_reason,  
    ri.return_quantity AS return_quantity  
FROM return_header rh  
JOIN return_item ri  
    ON rh.return_id = ri.return_id  
WHERE ri.order_id IN (  
    SELECT order_id  
    FROM return_item  
    GROUP BY order_id  
    HAVING COUNT(DISTINCT return_id) > 1  
);

```

---

## 7 Store with Most One-Day Shipped Orders (Last Month)

### Business Problem  
Identify which facility (store) handled the highest volume of “one-day shipping” orders in the previous month, useful for operational benchmarking.

```sql
SELECT f.FACILITY_ID,  
       f.FACILITY_NAME,  
       COUNT(DISTINCT oisg.order_id) AS TOTAL_ONE_DAY_SHIP_ORDERS,  
       DATE_FORMAT(DATE_SUB(CURRENT_DATE,INTERVAL 1 MONTH),'%Y-%m') AS REPORTING_PERIOD  
         
       FROM ORDER_ITEM_SHIP_GROUP oisg  
       JOIN FACILITY f   
       ON oisg.FACILITY_ID = f.FACILITY_ID  
       JOIN ORDER_HEADER oh   
       ON oisg.ORDER_ID = oh.ORDER_ID WHERE oisg.SHIPMENT_METHOD_TYPE_ID = 'NEXT_DAY'  
       AND oh.ORDER_DATE >= DATE_FORMAT(DATE_SUB(CURRENT_DATE, INTERVAL 1 MONTH),'%Y-%m-01') AND oh.ORDER_DATE < DATE_FORMAT(CURRENT_DATE, '%Y-%m-01')  
       AND oh.STATUS_ID = 'ORDER_COMPLETED'  
       GROUP by f.FACILITY_ID, f.FACILITY_NAME  
       ORDER BY TOTAL_ONE_DAY_SHIP_ORDERS DESC  
       LIMIT 1;

```



---

## 8 List of Warehouse Pickers

### Business Problem  
Warehouse managers need a list of employees responsible for picking and packing orders to manage shifts, productivity, and training needs.

```sql
SELECT fp.PARTY_ID,  
       CONCAT(p.FIRST_NAME,' ',p.LAST_NAME) AS NAME,  
       fp.ROLE_TYPE_ID,  
       fp.FACILITY_ID,  
       pty.STATUS_ID AS STATUS  
         
       FROM FACILITY_PARTY fp  
       JOIN PERSON p   
       ON fp.PARTY_ID = p.PARTY_ID  
       JOIN PARTY pty   
       ON fp.PARTY_ID = pty.PARTY_ID WHERE fp.ROLE_TYPE_ID = '%PICKER%'  
       AND (  
       fp.THRU_DATE IS NULL OR fp.THRU_DATE > CURRENT_TIMESTAMP  
       )  
         
       ORDER BY fp.FACILITY_ID , fp.ROLE_TYPE_ID, NAME;

```

---

## 9 Total Facilities That Sell the Product

### Business Problem  
Retailers want to see how many (and which) facilities (stores, warehouses, virtual sites) currently offer a product for sale.

```sql
SELECT   
    ii.PRODUCT_ID,  
    ii.FACILITY_ID,  
    f.FACILITY_TYPE_ID,  
    SUM(ii.QUANTITY_ON_HAND_TOTAL) AS QOH,  
    SUM(ii.AVAILABLE_TO_PROMISE_TOTAL) AS ATP  
    FROM   
    INVENTORY_ITEM ii  
    JOIN   
    FACILITY f ON ii.FACILITY_ID = f.FACILITY_ID  
    WHERE   
    f.FACILITY_TYPE_ID NOT IN (  
        SELECT FACILITY_TYPE_ID   
        FROM FACILITY_TYPE   
        WHERE FACILITY_TYPE_ID = 'VIRTUAL_FACILITY'   
           OR PARENT_TYPE_ID = 'VIRTUAL_FACILITY'  
    )  
    GROUP BY   
    ii.PRODUCT_ID,   
    ii.FACILITY_ID,   
    f.FACILITY_TYPE_ID;

```
       


---

## 10 Total Items in Various Virtual Facilities

### Business Problem  
Retailers need to study the relation of inventory levels of products to the type of facility it's stored at. Retrieve all inventory levels for products at locations and include the facility type Id. Do not retrieve facilities that are of type Virtual.

```sql
SELECT   
    ii.PRODUCT_ID,  
    ii.FACILITY_ID,  
    f.FACILITY_TYPE_ID,  
    SUM(ii.QUANTITY_ON_HAND_TOTAL) AS QOH,  
    SUM(ii.AVAILABLE_TO_PROMISE_TOTAL) AS ATP  
    FROM   
    INVENTORY_ITEM ii  
    JOIN   
    FACILITY f ON ii.FACILITY_ID = f.FACILITY_ID  
    WHERE   
    f.FACILITY_TYPE_ID NOT IN (  
        SELECT FACILITY_TYPE_ID   
        FROM FACILITY_TYPE   
        WHERE FACILITY_TYPE_ID = 'VIRTUAL_FACILITY'   
           OR PARENT_TYPE_ID = 'VIRTUAL_FACILITY'  
    )  
    GROUP BY   
    ii.PRODUCT_ID,   
    ii.FACILITY_ID,   
    f.FACILITY_TYPE_ID;
```
    


---

## 11 Transfer Orders Without Inventory Reservation

### Business Problem  
When transferring stock between facilities, the system should reserve inventory. If it isn’t reserved, the transfer may fail or oversell.

```sql
SELECT  
    oh.ORDER_ID AS TRANSFER_ORDER_ID,  
    oisg.FACILITY_ID AS FROM_FACILITY_ID,  
    oh.ORIGIN_FACILITY_ID AS TO_FACILITY_ID,  
    oi.PRODUCT_ID AS PRODUCT_ID,  
    oi.QUANTITY AS REQUESTED_QUANTITY,  
    COALESCE(SUM(oisgir.QUANTITY), 0) AS RESERVED_QUANTITY,  
    oh.ORDER_DATE AS TRANSFER_DATE,  
    oh.STATUS_ID AS STATUS  
FROM  
    ORDER_HEADER oh  
JOIN   
    ORDER_ITEM oi   
        ON oh.ORDER_ID = oi.ORDER_ID  
JOIN   
    ORDER_ITEM_SHIP_GROUP oisg   
        ON oh.ORDER_ID = oisg.ORDER_ID  
LEFT JOIN   
    ORDER_ITEM_SHIP_GRP_INV_RES oisgir   
        ON oi.ORDER_ID = oisgir.ORDER_ID   
        AND oi.ORDER_ITEM_SEQ_ID = oisgir.ORDER_ITEM_SEQ_ID   
        AND oisg.SHIP_GROUP_SEQ_ID = oisgir.SHIP_GROUP_SEQ_ID  
WHERE  
    oh.ORDER_TYPE_ID = 'TRANSFER_ORDER' AND oh.STATUS_ID NOT IN ('ORDER_COMPLETED', 'ORDER_CANCELLED', 'ORDER_REJECTED')  
    AND oi.STATUS_ID != 'ITEM_CANCELLED'  
GROUP BY  
    oh.ORDER_ID,  
    oisg.FACILITY_ID,  
    oh.ORIGIN_FACILITY_ID,  
    oi.PRODUCT_ID,  
    oi.QUANTITY,  
    oh.ORDER_DATE,  
    oh.STATUS_ID  
HAVING COALESCE(SUM(oisgir.QUANTITY), 0) < oi.QUANTITY;
```

---

## 12 Orders Without Picklist

### Business Problem  
A picklist is necessary for warehouse staff to gather items. Orders missing a picklist might be delayed and need attention.

```sql
SELECT  
    oh.ORDER_ID AS ORDER_ID,  
    oh.ORDER_DATE AS ORDER_DATE,  
    oh.STATUS_ID AS ORDER_STATUS,  
    oisg.FACILITY_ID AS FACILITY_ID,  
    TIMESTAMPDIFF(HOUR, oh.ORDER_DATE, CURRENT_TIMESTAMP) AS DURATION_IN_HOURS  
FROM  
    ORDER_HEADER oh  
JOIN   
    ORDER_ITEM_SHIP_GROUP oisg   
        ON oh.ORDER_ID = oisg.ORDER_ID  
LEFT JOIN   
    PICKLIST_ITEM pli   
        ON oh.ORDER_ID = pli.ORDER_ID   
        AND oisg.SHIP_GROUP_SEQ_ID = pli.SHIP_GROUP_SEQ_ID  
WHERE  
    oh.ORDER_TYPE_ID = 'SALES_ORDER'  
    AND oh.STATUS_ID = 'ORDER_APPROVED'   
    AND oisg.FACILITY_ID IS NOT NULL  
    AND pli.PICKLIST_BIN_ID IS NULL  
GROUP BY  
    oh.ORDER_ID,  
    oh.ORDER_DATE,  
    oh.STATUS_ID,  
    oisg.FACILITY_ID;
```
