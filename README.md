# Spottio 📱

**Spottio** è un'applicazione Android nativa sviluppata in **Java** che combina le dinamiche di un social network moderno (condivisione di post, feed algoritmico basato sugli interessi, interazioni e profili) con una piattaforma di messaggistica in tempo reale per chat private e gruppi. Il backend è interamente supportato da **Firebase** (Authentication e Cloud Firestore).

---

## 🚀 Architettura del Progetto e Componenti UI

L'applicazione segue un pattern architetturale in cui la logica (Java) è nettamente separata dalla presentazione (XML), con l'ausilio di Adapter per le liste e Helper/Manager per le funzioni globali.

### 🔐 1. Accesso, Autenticazione e Splash Screen
* **Punto di Ingresso (Splash)**: L'app parte da `SplashActivity`, che mostra il logo dell'app (`activity_splash.xml`) e, dopo 3 secondi, reindirizza al login eliminandosi dal back stack.
* **Gestione Interfaccia Main**: `MainActivity` si appoggia su `activity_main.xml`, che contiene un layout fluido con `TextInputLayout` per email, username e password, oltre a link per il recupero credenziali e l'informativa legale. L'helper `LoginUIHelper` gestisce dinamicamente la transizione visiva tra "Login" e "Registrazione" nascondendo o mostrando i relativi campi.
* **Autenticazione Cloud**: `AuthManager` gestisce la registrazione sicura e il login tramite Firebase Auth. Alla creazione dell'account, viene inizializzato un record base per l'utente nella collezione Firestore `users`.

### 📝 2. Social Feed, Creazione Post e Interazioni
* **Feed Algoritmico (`HomeFragment`)**: La bacheca principale (`fragment_home_feed.xml` / `fragment_home.xml`) ordina i post anteponendo le categorie preferite dall'utente salvate su Firestore. Include un sistema di scrolling infinito limitato a blocchi di 10 post per ottimizzare il traffico dati.
* **Visualizzazione Post (`item_post.xml`)**: Gestita da `PostAdapter`, ogni card del feed mostra l'avatar dell'autore, il testo, i contatori (Like/Commenti) e media allegati (immagini o `VideoView`).
* **Pubblicazione (`AddPostFragment`)**: L'interfaccia definita in `fragment_add_post.xml` permette agli utenti di scrivere testo, selezionare una categoria (da `categories` su Firebase) e allegare file multimediali catturati tramite launcher nativo.
* **Commenti (`dialog_comments.xml`)**: `PostUtils` crea un pannello a comparsa (`BottomSheetDialog`) popolato da `item_comment.xml` tramite `CommentAdapter`, permettendo l'invio rapido di feedback atomici con formattazione locale della data.

### 💬 3. Chat in Tempo Reale e Gruppi
* **Dashboard Chat (`MessageUsersFragment`)**: Sfruttando `fragment_messages.xml`, funge da hub centrale che elenca le chat attive recuperate dalla collezione `chat_previews`. Un menu contestuale consente di avviare chat singole o creare nuovi gruppi salvandoli nella collezione `groups`.
* **Rendering Lista Anteprime (`item_chat_preview.xml`)**: Ogni riga dell'elenco messaggi (`ChatPreviewAdapter`) espone un layout pulito con il nome, l'ultimo messaggio e un badge visivo dinamico: sfondo blu e iniziale per gli utenti, sfondo verde con icona 👥 per i gruppi.
* **Interfaccia Chat Attiva (`ChatFragment`)**: Sostenuta da `fragment_chat.xml`, aggiorna i messaggi in tempo reale agganciando uno snapshot listener a Firestore ed effettua l'auto-scroll in basso.
* **Design dei Messaggi (`item_message.xml`)**: `ChatAdapter` smista i messaggi: bolla verde a destra (inviati), bolla grigia a sinistra (ricevuti), con supporto al divisore cronologico temporale (`bg_date_divider`) e al nome del mittente nei contesti di gruppo.

### 👥 4. Profilo Utente, Relazioni e Moderazione
* **Il Pannello Profilo (`ProfileFragment`)**: L'UI (`fragment_profile.xml`) espone avatar, biografia, e contatori interattivi per Followers e Following. Un `ProfileViewModel` si occupa del fetch dei dati da Firebase, mentre un `ProfileCoordinator` instrada le transizioni a schermo.
* **Liste Follower/Seguiti (`FollowersFragment` / `FollowingFragment`)**: Entrambi utilizzano il layout riciclabile `fragment_user_list.xml` e la riga `item_user.xml` tramite il `UserAdapter` per mappare l'elenco della community con avatar e username.
* **Pannello Admin Segnalazioni (`AdminReportsActivity`)**: Accessibile dagli amministratori (`fragment_profile.xml`), questa activity (`activity_admin_reports.xml`) elenca le segnalazioni in tempo reale. La visualizzazione del singolo report è curata da `item_report.xml`, che permette all'admin di leggere la motivazione, estrarre il testo del post e cancellare istantaneamente il contenuto incriminato.
* **UI Segnalazione per Utenti (`dialog_report.xml`)**: Popup custom invocato da `PostUtils` con menu a tendina (`Spinner`) e campo descrizione per segnalare comportamenti abusivi.

### 🌐 5. Multilingua e Informativa Legale
* **Selettore Lingua (`LanguageSelectorManager`)**: Sfrutta la vista `layout_language_selector.xml` per agganciare un menu popup contenente 11 lingue con relative bandiere. Il cambio lingua ricrea il context dell'activity al volo tramite `LanguageHelper`.
* **Informativa Legale (`InfoActivity` / `InfoFragment`)**: Pagine di servizio modellate su `activity_info.xml` e `fragment_info.xml` che illustrano Privacy e Policy, il cui testo è generato dinamicamente a seconda del locale attivo. Il parsing di base sostituisce link HTML con tag cliccabili.

---

## 🛠️ Navigazione Principale (`HomeActivity`)

Il nucleo della User Experience è centralizzato in `HomeActivity` appoggiata al layout root `activity_home.xml`:
* **Navigazione a Schede (BottomNav + ViewPager2)**: Gestisce in orizzontale il Feed (Home), Ricerca Utenti, Aggiunta Post, Messaggi e Profilo Personale.
* **Gestione a Tutto Schermo (FrameLayout Overlay)**: Per la visualizzazione di Fragment secondari (come la pagina della Chat aperta o il Profilo di un altro utente), un contenitore (`@+id/fragment_container`) nascosto sovrasta il Pager. Un listener collegato al *Back Stack* riconosce la transizione e nasconde i menu principali, regalando spazio all'utente in modo immersivo.
* **Notifiche Push & Deep Linking**: Tramite `NotificationHelper` viene generata una notifica locale Android contenente il flag `target_user`. Al tap sulla notifica, la `HomeActivity` intercetta i metadati (tramite `onCreate` o `onNewIntent`) e inserisce istantaneamente un `ChatFragment` con quel mittente.

---

## 🗄️ Mappatura Database (Cloud Firestore)

L'app scala in tempo reale sui server Firebase grazie a queste collezioni (inferite dal codice Java):

```text
├── users (ID: username)
│   ├── email, bio, isAdmin, isSuspended, createdAt
│   ├── followers [array], following [array]
│   └── interests [map -> {categoria: punteggio}]
│
├── posts (ID: generato)
│   ├── user, text, mediaUri, isVideo, category, userPfpUri, timestamp
│   ├── likes [array di username]
│   └── comments [array di oggetti {author, text, timestamp}]
│
├── categories (ID: generato)
│   └── name
│
├── groups (ID: generato)
│   ├── name, createdBy
│   └── members [array di username]
│
├── chats (ID: generato)
│   ├── sender, text, timestamp
│   ├── (1vs1): receiver, conversationId
│   └── (Gruppo): groupId
│
├── chat_previews (ID: conversationId o groupId)
│   ├── lastMessage, lastSender, lastUpdate, isGroup
│   ├── (1vs1): participants [array]
│   └── (Gruppo): groupName, participants [array]
│
└── reports (ID: generato)
    └── postId, postAuthor, reporterUser, reason, description, postText, timestamp