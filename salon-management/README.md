# Salon Management (Spring Boot + Supabase + WhatsApp)

Features: login, dashboard, customers, services, appointments, WhatsApp messages (send + webhook receive).

## 1. Zaroorat
- Java 17 ya usse naya (aapke paas Java 25 hai, wo chalega)
- Maven (`mvn -v` se check karo)

## 2. Sabse pehle bina Supabase ke test karo
Terminal mein is folder ke andar (jahan `pom.xml` hai) ye chalao:

    mvn spring-boot:run

Browser: http://localhost:8080  -> login: `admin` / `admin123`

`.env` nahi hai to app local H2 memory database use karti hai (data restart par ud jata hai).

## 3. Supabase connect karo
1. `.env.example` ko copy karke naam `.env` rakho (isi folder mein, `pom.xml` ke bagal mein).
2. Supabase Dashboard -> **Project Settings -> Database -> Connection string** (JDBC) se ye 3 values bharo:
   - `DB_URL`
   - `DB_USERNAME`
   - `DB_PASSWORD`
3. Dubara `mvn spring-boot:run`. Tables apne aap ban jati hain (Hibernate `ddl-auto=update`).

Tips:
- IPv4 network par **Session pooler** (port 5432, username `postgres.<project-ref>`) use karo.
- **Transaction pooler (port 6543)** use karna ho to DB_URL ke end mein `?sslmode=require&prepareThreshold=0` lagao.
- `DB_URL` hamesha `jdbc:postgresql://` se shuru hona chahiye.

## 4. WhatsApp connect karo (Meta Cloud API)
1. https://developers.facebook.com par app banao -> **WhatsApp** product add karo.
2. **API Setup** page se `Phone number ID` aur access token lo. `.env` mein daalo:
   `WHATSAPP_TOKEN`, `WHATSAPP_PHONE_NUMBER_ID`.
   (Temporary token 24 ghante chalta hai. Permanent ke liye Business Settings -> System User banao.)
3. **Incoming messages (webhook)** ke liye app public URL par honi chahiye (local par `ngrok http 8080`).
   Meta -> WhatsApp -> Configuration mein:
   - Callback URL: `https://<tumhara-url>/webhook/whatsapp`
   - Verify token: wahi jo `.env` ke `WHATSAPP_VERIFY_TOKEN` mein hai
   - `messages` field subscribe karo.
4. Zaroori rule: customer ne pichhle 24 ghante mein message nahi kiya ho to sirf **approved template** hi jaata hai.
   Template ka naam `WHATSAPP_CONFIRM_TEMPLATE` mein daalo. Template body mein 3 variables hone chahiye:
   `{{1}}` naam, `{{2}}` service, `{{3}}` samay. Test ke liye Meta ka `hello_world` template bina variables ke chalta hai.
   (Test mode mein recipient number Meta dashboard mein pehle add karna padta hai.)

## 5. Production se pehle
- `.env` mein `ADMIN_PASSWORD` badlo.
- `.env` ko kabhi git mein push mat karo (`.gitignore` mein already hai).
- Public internet par deploy karo to CSRF protection add karna.

## Folder
- `src/main/java/com/legacy/salon/` : Java code (model, repo, service, web, config)
- `src/main/resources/static/` : login.html, index.html, app.js, style.css
- `src/main/resources/application.properties` : settings
