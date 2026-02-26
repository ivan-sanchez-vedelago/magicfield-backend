Uploading products with images

Overview
--------
The backend accepts product creation with optional images via multipart/form-data at:

- POST http://localhost:8080/api/products
- POST http://localhost:8080/api/products/addProduct

Curl example
------------
Use the `product` part as JSON and one or more `images` file parts:

```bash
curl -v -X POST "http://localhost:8080/api/products" \
  -F 'product={"name":"Camiseta","description":"Camiseta de algodón","price":19.99,"stock":50};type=application/json' \
  -F "images=@/path/to/img1.jpg" \
  -F "images=@/path/to/img2.png"
```

Postman (GUI) steps
--------------------
1. Set method to `POST` and URL to `http://localhost:8080/api/products`.
2. In `Body` select `form-data`.
3. Add a key named `product`, set its type to `Text` and paste a JSON object:

   {"name":"Camiseta","description":"...","price":19.99,"stock":50}

4. Add one or more keys named `images`, set each to type `File` and choose an image file.
5. Send the request. Successful response: HTTP 201 Created and JSON product data.

Notes
-----
- The `product` part must be valid JSON matching `ProductRequest` fields.
- `images` is optional; files are stored as LOBs in the `images` table.
- For testing, you can also POST to `/api/products/addProduct` (alias).
