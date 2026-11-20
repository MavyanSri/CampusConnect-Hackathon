# MediFlow backend ni local ga run cheyadam

1. Eclipse lo running app unte Console lo red Stop button click cheyi. ZIP ni separate folder lo extract cheyi.
2. ZIP extract chesaka src, pom.xml, Dockerfile, postman, RUN-MEDIFLOW.md kanipinche folder lo unna items ni existing CampusConnect-Hackathon/backend loki copy cheyi. Existing project folder ni delete cheyyalsina avasaram ledu. Ee package lo full Java files, pom.xml, application.properties unnayi.
3. Eclipse lo backend project select chesi F5 press cheyi. Right-click > Maven > Update Project > OK. JDK 25 select ayi undali.
4. Run > Run Configurations > nee BackendApplication > Environment lo MEDIFLOW_DB_PASSWORD ki nee local MySQL password ivvu. Existing MEDIFLOW_JWT_SECRET undali (fresh, private 32 random bytes encoded as Base64). DB name mediflow_db, username root, port 3306 defaults. Different ayithe MEDIFLOW_DB_USER, MEDIFLOW_DB_URL pettu. Database already unte ade use avuthundi. Fresh database kosam MySQL lo CREATE DATABASE mediflow_db; run cheyi.
5. Nee existing admin account use cheyochu. Fresh demo DB lo admin create cheyadaniki MEDIFLOW_DEMO_ADMIN_PASSWORD (minimum 12 characters) first start ki set cheyi. Login email admin.demo@example.com. First startup tarvata aa optional variable remove cheyochu.
6. Apply > Run. Console lo Started BackendApplication vachaka backend ready. Port 8080 busy unte paatha instance stop cheyi.
7. Maven package kosam existing backend folder lo PowerShell open chesi .\mvnw.cmd clean package run cheyi. Wrapper available kaakapothe Eclipse > Run As > Maven build... > Goals: clean package. Ee source ZIP ki Eclipse Maven saripothundi.
8. React local URLs http://localhost:5173, http://localhost:3000 allow avuthayi. Vere frontend origin kosam MEDIFLOW_FRONTEND_ORIGINS lo comma-separated full origins ivvu. Frontend requests base URL http://localhost:8080/api.

## Render deployment

Git repo lo root directory backend, runtime Docker select cheyi. Dockerfile JDK 25 tho Maven build chestundi. External MySQL DB URL, username, password environment lo set cheyi. URL jdbc:mysql://HOST:PORT/DB format lo provider ichina TLS parameters tho ivvu. MEDIFLOW_JWT_SECRET ki private deployment key, MEDIFLOW_FRONTEND_ORIGINS ki frontend HTTPS origin ivvu. PORT platform nunchi vastundi. Deploy tarvata frontend API base URL ni deployed backend URL + /api ki set cheyi.

## Postman collections
Corrected backend start ayyaka postman/MediFlow-Fixed-30.postman_collection.json import cheyi. Paatha 15-route backend test kosam maatrame Original-15 collection use cheyi (active specialization okati munduga undali). MediFlow Local environment lo adminPassword set chesi select cheyi. 01 nunchi order lo Send nokku. Demo records local DB lo create avuthayi.

## Postman checks (app start ayyaka)

- Patient register, login cheyi. Protected requests lo Authorization > Bearer Token lo accessToken okkate pettu.
- Admin login tho specialization create cheyi; doctor account create cheyi.
- Doctor login tho weekly availability add cheyi. Aa weekday ki future date enchuko.
- Patient ga slots fetch chesi oka slot book cheyi. Ade slot malli book cheste 409 ravali. My appointments lo booking kanapadali.
- Confirmed booking meeda overlap ayye time off add cheste 409 ravali. Patient cancel chesaka aa future slot malli available avvali.
- Doctor ga tana booking matrame manage cheyagalali. Start time daatina confirmed booking ni complete cheyochu.
- Admin ga dashboard, doctors, patients, appointments, analytics check cheyi. Patient token tho admin dashboard ki 403 ravali.
