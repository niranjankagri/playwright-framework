# Manual test cases

The test cases this framework automates, written as a tester would run them by hand. Every case has
an ID; the TestNG method that covers it starts with the same ID (`tc101_standardUserSeesProducts`) and
its description too (`[TC-101] Standard user logs in and sees the products`), so a failure in the
report leads straight to the case below.

| ID range | Area | Automated in |
|---|---|---|
| TC-101 to TC-123 | Sauce Demo shop in the browser (UI, network, sessions) | `tests/ui`, `tests/network`, `tests/interview/PlaywrightScenariosTest` |
| TC-201 to TC-213 | Restful Booker REST API | `tests/api`, `tests/interview/TestNgFeaturesTest` |

A case may be automated by more than one method: `TestNgFeaturesTest` shows TestNG features
(priority, dependsOnMethods, retryAnalyzer, @Parameters ...) on checks that `tests/api` also makes,
so those methods carry the ID of the same case. Data-driven methods (TestNG data providers) are one
case with several data sets.

Not listed, on purpose: `tests/framework/PageFactoryTest` (checks the framework's own page factory),
`tests/demo/ReportDemoTest` (fails and skips on purpose for the report), and three Playwright
technique demos in `PlaywrightScenariosTest` that do not test the shop (five locator strategies, an
element screenshot, a JavaScript dialog the test opens itself).

## Setup for manual runs

- Shop: <https://www.saucedemo.com>, password `secret_sauce` for every user: `standard_user`,
  `locked_out_user`, `problem_user`.
- API: <https://restful-booker.herokuapp.com>, admin user `admin` / `password123`. A public demo that
  resets itself regularly; create your own booking for each case.
- Products used: Sauce Labs Backpack, Sauce Labs Bike Light, Sauce Labs Bolt T-Shirt.

## Summary

| ID | Title | Type | Priority | Automated by |
|---|---|---|---|---|
| TC-101 | Standard user logs in and sees the products | Smoke | High | `LoginTest.tc101_standardUserSeesProducts` |
| TC-102 | Rejected logins show the right message (5 data sets) | Negative | High | `LoginTest.tc102_loginIsRejected` |
| TC-103 | User logs out and returns to the login page | Smoke | High | `LoginTest.tc103_userCanLogOut` |
| TC-104 | Back after logout does not show the products | Security | High | `LoginTest.tc104_...` |
| TC-105 | The product list requires a login | Security | High | `LoginTest.tc105_productListRequiresLogin` |
| TC-106 | Products are sorted (4 sort options) | Regression | Medium | `InventoryTest.tc106_productsAreSorted` |
| TC-107 | Every product has a name and a positive price | Regression | Medium | `InventoryTest.tc107_...` |
| TC-108 | Product details match the list | Smoke | High | `InventoryTest.tc108_productDetailsMatchTheList` |
| TC-109 | Added products appear in the cart | Smoke | High | `CartTest.tc109_...` |
| TC-110 | Removing the only product empties the cart | Regression | Medium | `CartTest.tc110_...` |
| TC-111 | The cart survives a page reload | Regression | Medium | `CartTest.tc111_cartSurvivesAReload` |
| TC-112 | A full purchase adds up and completes | Smoke / E2E | High | `CheckoutTest.tc112_...` |
| TC-113 | Checkout needs every buyer field (3 data sets) | Negative | High | `CheckoutTest.tc113_...` |
| TC-114 | Cancelling the checkout keeps the cart | Regression | Medium | `CheckoutTest.tc114_...` |
| TC-115 | The shop works with images blocked | Network | Low | `NetworkTest.tc115_...` |
| TC-116 | No failing requests while shopping | Network / Smoke | High | `NetworkTest.tc116_...` |
| TC-117 | A mocked API response replaces the server's | Network | Low | `NetworkTest.tc117_...` |
| TC-118 | Login works with the keyboard only | Accessibility | Medium | `PlaywrightScenariosTest.tc118_...` |
| TC-119 | A second tab shares the login | Session | Medium | `PlaywrightScenariosTest.tc119_...` |
| TC-120 | A saved login skips the login form | Session | Medium | `PlaywrightScenariosTest.tc120_...` |
| TC-121 | The cart is kept in local storage | Regression | Low | `PlaywrightScenariosTest.tc121_...` |
| TC-122 | Logging in loads the product images | Network | Low | `PlaywrightScenariosTest.tc122_...` |
| TC-123 | Product images are not broken, except for the problem user | Regression | Medium | `PlaywrightScenariosTest.tc123_...` |
| TC-201 | API health check answers | API / Smoke | High | `AuthApiTest.tc201_healthCheckAnswers`, `TestNgFeaturesTest.tc201_apiIsUp` |
| TC-202 | Admin gets an auth token | API | High | `AuthApiTest.tc202_adminGetsAToken`, `TestNgFeaturesTest.tc202_adminGetsATokenOnceTheApiIsUp`, `tc202_suiteParameterUserGetsAToken` |
| TC-203 | A wrong password gets no token | API / Negative | High | `AuthApiTest.tc203_wrongPasswordGetsNoToken`, `TestNgFeaturesTest.tc203_wrongPasswordThrowsTheExpectedException` |
| TC-204 | A created booking can be read back | API / Smoke | High | `BookingApiTest.tc204_...` |
| TC-205 | A booking is found by name | API | Medium | `BookingApiTest.tc205_bookingIsFoundByName` |
| TC-206 | PUT replaces a booking | API | High | `BookingApiTest.tc206_putReplacesABooking` |
| TC-207 | PATCH changes only the given fields | API | Medium | `BookingApiTest.tc207_...` |
| TC-208 | Changes without a token are refused (403) | API / Security | High | `BookingApiTest.tc208_changesNeedAToken` |
| TC-209 | A deleted booking is gone | API | High | `BookingApiTest.tc209_deletedBookingIsGone` |
| TC-210 | An unknown booking is not found (404) | API / Negative | Low | `BookingApiTest.tc210_...`, `TestNgFeaturesTest.tc210_...WithRetry` |
| TC-211 | Health check and login answer together, login within 30 s | API | Medium | `TestNgFeaturesTest.tc211_softAssertChecksEverything` |
| TC-212 | The health check answers on repeated calls | API / Reliability | Low | `TestNgFeaturesTest.tc212_healthCheckAnswersEveryTime` (runs twice) |
| TC-213 | A token arrives within 30 seconds | API / Performance | Low | `TestNgFeaturesTest.tc213_tokenArrivesWithinThirtySeconds` |

---

## UI: login (`LoginTest`)

### TC-101 Standard user logs in and sees the products

| # | Step | Expected result |
|---|---|---|
| 1 | Open the shop, log in as `standard_user` / `secret_sauce` | URL ends with `/inventory.html` |
| 2 | Look at the page | Title "Products"; 6 products |

### TC-102 Rejected logins show the right message

For every data set: open the login page, enter the user and password, click **Login**. Expected: the
message below, and the **Login** button is still shown.

| Data set | User | Password | Message |
|---|---|---|---|
| wrong password | `standard_user` | `wrong_password` | Epic sadface: Username and password do not match any user in this service |
| unknown user | `no_such_user` | `secret_sauce` | Epic sadface: Username and password do not match any user in this service |
| missing username | (empty) | `secret_sauce` | Epic sadface: Username is required |
| missing password | `standard_user` | (empty) | Epic sadface: Password is required |
| locked-out user | `locked_out_user` | `secret_sauce` | Epic sadface: Sorry, this user has been locked out. |

### TC-103 User logs out and returns to the login page

| # | Step | Expected result |
|---|---|---|
| 1 | Log in as `standard_user` | Products page |
| 2 | Open the menu (☰), click **Logout** | Login page with **Login** button; URL is no longer `/inventory.html` |

### TC-104 Back after logout does not show the products

| # | Step | Expected result |
|---|---|---|
| 1 | Log in, log out (TC-103) | Login page |
| 2 | Press the browser's **Back** button | Still the login page, with a message containing "when you are logged in" |

### TC-105 The product list requires a login

| # | Step | Expected result |
|---|---|---|
| 1 | Logged out, open `https://www.saucedemo.com/inventory.html` directly | Login page: "Epic sadface: You can only access '/inventory.html' when you are logged in." |

## UI: products (`InventoryTest`)

Preconditions for all: logged in as `standard_user`.

### TC-106 Products are sorted

For every data set: choose the option in the sort list. Expected: the order below.

| Data set | Option | Expected order |
|---|---|---|
| name A to Z | Name (A to Z) | Names alphabetical |
| name Z to A | Name (Z to A) | Names reverse alphabetical |
| price low to high | Price (low to high) | Prices ascending |
| price high to low | Price (high to low) | Prices descending |

### TC-107 Every product has a name and a positive price

| # | Step | Expected result |
|---|---|---|
| 1 | Look at the product list | 6 products; every one has a name and a price above 0 |

### TC-108 Product details match the list

| # | Step | Expected result |
|---|---|---|
| 1 | Note the price of Sauce Labs Backpack in the list; open the product | Detail page: same name, same price |
| 2 | Click **Back to products** | Product list with 6 products |

## UI: cart (`CartTest`)

Preconditions for all: logged in as `standard_user`, cart empty.

### TC-109 Added products appear in the cart

| # | Step | Expected result |
|---|---|---|
| 1 | **Add to cart** on Sauce Labs Backpack and Sauce Labs Bike Light | Cart badge `2` |
| 2 | Open the cart | Title "Your Cart"; Backpack, then Bike Light |

### TC-110 Removing the only product empties the cart

| # | Step | Expected result |
|---|---|---|
| 1 | Add the Backpack | Badge `1` |
| 2 | Click **Remove** on it | Badge gone |
| 3 | Open the cart | No items |

### TC-111 The cart survives a page reload

| # | Step | Expected result |
|---|---|---|
| 1 | Add Sauce Labs Bolt T-Shirt, reload the page (F5) | Badge still `1` |

## UI: checkout (`CheckoutTest`)

Preconditions for all: logged in as `standard_user`, cart empty.

### TC-112 A full purchase adds up and completes

| # | Step | Expected result |
|---|---|---|
| 1 | Note the list prices of Backpack and Bolt T-Shirt; add both; open the cart; **Checkout** | "Checkout: Your Information" |
| 2 | First name `Play`, last name `Wright`, postal code `10115`, **Continue** | Overview lists Backpack, then Bolt T-Shirt |
| 3 | Check the amounts | Item total = sum of the two list prices; Total = item total + tax (to the cent) |
| 4 | **Finish** | "Thank you for your order!"; cart badge gone |

### TC-113 Checkout needs every buyer field

For every data set: add the Backpack, open the cart, **Checkout**, fill the fields, **Continue**.
Expected: the message below, and the page is still "Checkout: Your Information".

| Data set | First name | Last name | Postal code | Message |
|---|---|---|---|---|
| missing first name | (empty) | `Wright` | `10115` | Error: First Name is required |
| missing last name | `Play` | (empty) | `10115` | Error: Last Name is required |
| missing postal code | `Play` | `Wright` | (empty) | Error: Postal Code is required |

### TC-114 Cancelling the checkout keeps the cart

| # | Step | Expected result |
|---|---|---|
| 1 | Add the Backpack, open the cart, **Checkout**, then **Cancel** | "Your Cart" with the Backpack; badge `1` |

## UI: network (`NetworkTest`)

### TC-115 The shop works with images blocked

| # | Step | Expected result |
|---|---|---|
| 1 | DevTools › Network › block request patterns `*.png`, `*.jpg`, `*.svg`; log in | 6 products shown, without pictures |
| 2 | Check the first product image | Not loaded (naturalWidth `0`); blocked requests listed in DevTools |

### TC-116 No failing requests while shopping

| # | Step | Expected result |
|---|---|---|
| 1 | DevTools › Network open; log in, add the Backpack, open the cart | Requests to `www.saucedemo.com` answered; none with status 400 or higher, none failed (cancelled requests excepted) |

### TC-117 A mocked API response replaces the server's

Needs a proxy or DevTools **Override content**.

| # | Step | Expected result |
|---|---|---|
| 1 | Override `https://restful-booker.herokuapp.com/booking/1` with `{"firstname":"Mocked","lastname":"By Playwright","totalprice":1}`, open that URL | The browser shows the mocked JSON ("Mocked", "By Playwright") |

## UI: sessions and pages (`PlaywrightScenariosTest`)

### TC-118 Login works with the keyboard only

| # | Step | Expected result |
|---|---|---|
| 1 | Click into the user name field; type `standard_user`, Tab, `secret_sauce`, Enter | Products page with 6 products (no mouse needed) |

### TC-119 A second tab shares the login

| # | Step | Expected result |
|---|---|---|
| 1 | Log in; open `/inventory.html` in a new tab of the same window | The new tab shows the 6 products without logging in |

### TC-120 A saved login skips the login form

| # | Step | Expected result |
|---|---|---|
| 1 | Log in; in DevTools › Application copy the `session-username` cookie | |
| 2 | In a fresh private window set the same cookie on `www.saucedemo.com`, open `/inventory.html` | 6 products, no login form |

### TC-121 The cart is kept in local storage

| # | Step | Expected result |
|---|---|---|
| 1 | Log in, add the Backpack | |
| 2 | DevTools › Application › Local Storage | `cart-contents` = `[4]` (the Backpack's id); the page is fully loaded (`document.readyState` `complete`) |

### TC-122 Logging in loads the product images

| # | Step | Expected result |
|---|---|---|
| 1 | DevTools › Network › Img; log in | The first product image request answers `200` |

### TC-123 Product images are not broken, except for the problem user

| # | Step | Expected result |
|---|---|---|
| 1 | Log in as `standard_user`, look at the 6 product images | 6 different pictures, every one loads (`200`) |
| 2 | Log out, log in as `problem_user` | Every product shows the same (wrong) picture: the known bug of this demo user |

---

## API: auth (`AuthApiTest`, `TestNgFeaturesTest`)

Base URL `https://restful-booker.herokuapp.com`; bodies are JSON.

### TC-201 API health check answers

| # | Step | Expected result |
|---|---|---|
| 1 | `GET /ping` | `201` (this API answers 201 Created to a ping) |

### TC-202 Admin gets an auth token

| # | Step | Expected result |
|---|---|---|
| 1 | `POST /auth` `{"username":"admin","password":"password123"}` | `200`, a non-empty `token` |

### TC-203 A wrong password gets no token

| # | Step | Expected result |
|---|---|---|
| 1 | `POST /auth` with password `wrong-password` | `200` (the API does not use 401), `{"reason":"Bad credentials"}`, no `token` |

### TC-211 Health check and login answer together, login within 30 s

| # | Step | Expected result |
|---|---|---|
| 1 | `GET /ping`, then `POST /auth` as admin, timing the second call | `201`; `200` with a token; the token call takes under 30 s. Note **every** deviation before failing the case |

### TC-212 The health check answers on repeated calls

| # | Step | Expected result |
|---|---|---|
| 1 | `GET /ping` twice in a row | `201` both times |

### TC-213 A token arrives within 30 seconds

| # | Step | Expected result |
|---|---|---|
| 1 | `POST /auth` as admin with a stopwatch | A token within 30 s |

## API: bookings (`BookingApiTest`)

Test data for a new booking: first name `Playwright`, last name `Test` + 8 random digits, total price
100-999, deposit paid `true`, check-in 30-90 days from today, check-out 3 days later, additional needs
`Breakfast`. Changes need the admin token as cookie `token=<token>`.

### TC-204 A created booking can be read back

| # | Step | Expected result |
|---|---|---|
| 1 | `POST /booking` with the new booking | `200`; `bookingid` > 0; `booking` equals what was sent |
| 2 | `GET /booking/<bookingid>` | `200`; the same booking |

### TC-205 A booking is found by name

| # | Step | Expected result |
|---|---|---|
| 1 | Create a booking; `GET /booking?firstname=Playwright&lastname=<last name>` | `200`; the list contains its `bookingid` |

### TC-206 PUT replaces a booking

| # | Step | Expected result |
|---|---|---|
| 1 | Create a booking; `PUT /booking/<id>` with first name `Updated` and total price + 50, with the token | `200`; the answer is the changed booking |
| 2 | `GET /booking/<id>` | The changed booking |

### TC-207 PATCH changes only the given fields

| # | Step | Expected result |
|---|---|---|
| 1 | Create a booking; `PATCH /booking/<id>` `{"firstname":"Patched"}` with the token | `200`; only the first name changed |

### TC-208 Changes without a token are refused (403)

| # | Step | Expected result |
|---|---|---|
| 1 | Create a booking; `PATCH /booking/<id>` `{"firstname":"Hacker"}` without a token | `403` |
| 2 | `DELETE /booking/<id>` without a token | `403` |
| 3 | `GET /booking/<id>` | Unchanged |

### TC-209 A deleted booking is gone

| # | Step | Expected result |
|---|---|---|
| 1 | Create a booking; `DELETE /booking/<id>` with the token | `201` (this API answers 201 to a delete) |
| 2 | `GET /booking/<id>` | `404` |

### TC-210 An unknown booking is not found (404)

| # | Step | Expected result |
|---|---|---|
| 1 | `GET /booking/2147483647` (or any id that does not exist) | `404` |
