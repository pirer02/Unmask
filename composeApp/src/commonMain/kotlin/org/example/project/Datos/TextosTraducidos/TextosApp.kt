package org.example.project.Datos.TextosTraducidos

import org.example.project.Datos.IdiomaSoportado

data class TextosApp(
    val menuInicio: String, val menuBiblioteca: String, val menuExplorar: String, val menuPerfil: String,
    val colabTitulo: String, val colabDesc1: String, val colabDesc2: String, val colabDesc3: String,
    val colabAceptar: String, val colabRechazar: String, val colabExito: String,
    val tut0Titulo: String, val tut0Desc: String, val tut0Btn: String,
    val tut1Titulo: String, val tut1Desc: String,
    val tut2Titulo: String, val tut2Desc: String,
    val tut3Titulo: String, val tut3Desc: String,
    val tut4JugTitulo: String, val tut4JugDesc: String,
    val tut4ConfTitulo: String, val tut4ConfDesc: String,
    val tut5Titulo: String, val tut5Desc: String,
    val tut6Titulo: String, val tut6Desc: String,
    val tut7Titulo: String, val tut7Desc: String,
    val tut8Titulo: String, val tut8Desc: String, val tut8Btn: String,
    val tutOmitir: String
)

fun obtenerTextosApp(idioma: IdiomaSoportado): TextosApp {
    return when (idioma) {
        IdiomaSoportado.ESPANOL -> TextosApp(
            menuInicio = "INICIO", menuBiblioteca = "BIBLIOTECA", menuExplorar = "EXPLORAR", menuPerfil = "PERFIL",
            colabTitulo = "¡Nueva Colaboración!", colabDesc1 = "El investigador @", colabDesc2 = " quiere que le ayudes a rellenar su lista '", colabDesc3 = "'.\n\nSi aceptas, podrás editarla y aparecerá en tu biblioteca como propia.",
            colabAceptar = "ACEPTAR", colabRechazar = "RECHAZAR", colabExito = "¡Ahora eres colaborador!",
            tut0Titulo = "🕵️ ¡Bienvenido!", tut0Desc = "Unmask es la app definitiva para jugar al impostor. El secreto está en las listas: ¡crea las tuyas!", tut0Btn = "¡ENTENDIDO!",
            tut1Titulo = "🆕 Crea tu lista", tut1Desc = "Pulsa 'CREAR AHORA' o el '+' de abajo para empezar. Haremos una de prueba.",
            tut2Titulo = "📝 El Taller", tut2Desc = "Ponle nombre y añade al menos 3 palabras con pistas para guardarla.",
            tut3Titulo = "📚 Tu Biblioteca", tut3Desc = "¡Genial! Aquí están tus creaciones. Pulsa 'JUGAR' para ver cómo se configura.",
            tut4JugTitulo = "👥 Añade amigos", tut4JugDesc = "Escribe sus nombres y pulsa '+'. Necesitas al menos 3 para jugar.\nLuego vuelve atrás.",
            tut4ConfTitulo = "⚙️ Ajustes de partida", tut4ConfDesc = "Añade mínimo 3 jugadores arriba y ajusta las reglas.\n\n¡Inicia cuando estés listo!",
            tut5Titulo = "🎭 Tu Rol", tut5Desc = "Pasad el móvil. Deslizad la carta para ver si sois CIVIL o IMPOSTOR.",
            tut6Titulo = "🗣️ El Debate", tut6Desc = "Hablad y usad las pistas para descubrir al impostor sin decir la palabra. ¡Votad al acabar!",
            tut7Titulo = "🏆 Resultados", tut7Desc = "Comprueba quién ha ganado. Pulsa 'FINALIZAR PARTIDA' abajo para terminar.",
            tut8Titulo = "🌎 Comunidad", tut8Desc = "¡Ya sabes lo básico! En 'Explorar' puedes bajar listas de otros. ¡Inicia sesión y comparte!", tut8Btn = "FINALIZAR",
            tutOmitir = "OMITIR TUTORIAL"
        )
        IdiomaSoportado.FRANCES -> TextosApp(
            menuInicio = "ACCUEIL", menuBiblioteca = "BIBLIOTHÈQUE", menuExplorar = "EXPLORER", menuPerfil = "PROFIL",
            colabTitulo = "Nouvelle Collaboration !", colabDesc1 = "L'enquêteur @", colabDesc2 = " veut votre aide pour sa liste '", colabDesc3 = "'.\n\nSi vous acceptez, vous pourrez l'éditer.",
            colabAceptar = "ACCEPTER", colabRechazar = "REFUSER", colabExito = "Vous êtes maintenant collaborateur !",
            tut0Titulo = "🕵️ Bienvenue !", tut0Desc = "Unmask est le jeu d'imposteur ultime. Le secret est dans les listes : créez les vôtres !", tut0Btn = "COMPRIS !",
            tut1Titulo = "🆕 Créez votre liste", tut1Desc = "Appuyez sur 'CRÉER MAINTENANT' ou sur le '+' en bas. Faisons un test.",
            tut2Titulo = "📝 L'Atelier", tut2Desc = "Donnez-lui un nom et ajoutez au moins 3 mots avec indices pour la sauvegarder.",
            tut3Titulo = "📚 Votre Bibliothèque", tut3Desc = "Super ! Voici vos créations. Appuyez sur 'JOUER' pour la configuration.",
            tut4JugTitulo = "👥 Ajoutez des amis", tut4JugDesc = "Écrivez leurs noms et appuyez sur '+'. Il en faut 3 minimum.\nPuis revenez.",
            tut4ConfTitulo = "⚙️ Réglages", tut4ConfDesc = "Ajoutez 3 joueurs en haut et ajustez les règles.\n\nCommencez quand vous êtes prêt !",
            tut5Titulo = "🎭 Votre Rôle", tut5Desc = "Faites passer le téléphone. Glissez la carte pour voir si vous êtes CIVIL ou IMPOSTEUR.",
            tut6Titulo = "🗣️ Le Débat", tut6Desc = "Parlez et utilisez les indices pour démasquer l'imposteur. Votez à la fin !",
            tut7Titulo = "🏆 Résultats", tut7Desc = "Vérifiez qui a gagné. Appuyez sur 'TERMINER' en bas.",
            tut8Titulo = "🌎 Communauté", tut8Desc = "Vous connaissez les bases ! Dans 'Explorer', téléchargez d'autres listes. Connectez-vous !", tut8Btn = "TERMINER",
            tutOmitir = "PASSER LE TUTO"
        )
        IdiomaSoportado.ITALIANO -> TextosApp(
            menuInicio = "HOME", menuBiblioteca = "LIBRERIA", menuExplorar = "ESPLORA", menuPerfil = "PROFILO",
            colabTitulo = "Nuova Collaborazione!", colabDesc1 = "L'investigatore @", colabDesc2 = " vuole il tuo aiuto per la lista '", colabDesc3 = "'.\n\nSe accetti, potrai modificarla.",
            colabAceptar = "ACCETTA", colabRechazar = "RIFIUTA", colabExito = "Ora sei un collaboratore!",
            tut0Titulo = "🕵️ Benvenuto!", tut0Desc = "Unmask è il gioco definitivo. Il segreto sono le liste: crea le tue!", tut0Btn = "CAPITO!",
            tut1Titulo = "🆕 Crea la tua lista", tut1Desc = "Premi 'CREA ORA' o il '+' in basso. Facciamo una prova.",
            tut2Titulo = "📝 Il Laboratorio", tut2Desc = "Dalle un nome e aggiungi almeno 3 parole per salvarla.",
            tut3Titulo = "📚 La tua Libreria", tut3Desc = "Ottimo! Ecco le tue creazioni. Premi 'GIOCA' per configurarla.",
            tut4JugTitulo = "👥 Aggiungi amici", tut4JugDesc = "Scrivi i nomi e premi '+'. Ne servono almeno 3.\nPoi torna indietro.",
            tut4ConfTitulo = "⚙️ Impostazioni", tut4ConfDesc = "Aggiungi 3 giocatori in alto e regola le regole.\n\nInizia quando sei pronto!",
            tut5Titulo = "🎭 Il tuo Ruolo", tut5Desc = "Passatevi il telefono. Scorri la carta per vedere se sei CIVILE o IMPOSTORE.",
            tut6Titulo = "🗣️ Il Dibattito", tut6Desc = "Parlate e usate gli indizi per scoprire l'impostore. Votate alla fine!",
            tut7Titulo = "🏆 Risultati", tut7Desc = "Controlla chi ha vinto. Premi 'TERMINA' in basso.",
            tut8Titulo = "🌎 Community", tut8Desc = "Ora sai le basi! In 'Esplora' puoi scaricare altre liste. Accedi e condividi!", tut8Btn = "TERMINA",
            tutOmitir = "SALTA TUTORIAL"
        )
        IdiomaSoportado.ALEMAN -> TextosApp(
            menuInicio = "START", menuBiblioteca = "BIBLIOTHEK", menuExplorar = "ENTDECKEN", menuPerfil = "PROFIL",
            colabTitulo = "Neue Kollaboration!", colabDesc1 = "Der Ermittler @", colabDesc2 = " möchte deine Hilfe für die Liste '", colabDesc3 = "'.\n\nWenn du zustimmst, kannst du sie bearbeiten.",
            colabAceptar = "AKZEPTIEREN", colabRechazar = "ABLEHNEN", colabExito = "Du bist jetzt ein Mitarbeiter!",
            tut0Titulo = "🕵️ Willkommen!", tut0Desc = "Unmask ist das ultimative Spiel. Das Geheimnis sind die Listen: Erstelle deine eigenen!", tut0Btn = "VERSTANDEN!",
            tut1Titulo = "🆕 Liste erstellen", tut1Desc = "Drücke 'JETZT ERSTELLEN' oder das '+'. Lass uns eine zum Testen machen.",
            tut2Titulo = "📝 Die Werkstatt", tut2Desc = "Gib ihr einen Namen und füge mindestens 3 Wörter hinzu, um sie zu speichern.",
            tut3Titulo = "📚 Deine Bibliothek", tut3Desc = "Super! Hier sind deine Kreationen. Drücke 'SPIELEN' zur Einrichtung.",
            tut4JugTitulo = "👥 Freunde hinzufügen", tut4JugDesc = "Schreibe Namen und drücke '+'. Mindestens 3 nötig.\nDann zurück.",
            tut4ConfTitulo = "⚙️ Einstellungen", tut4ConfDesc = "Füge oben 3 Spieler hinzu und passe Regeln an.\n\nStarte, wenn du bereit bist!",
            tut5Titulo = "🎭 Deine Rolle", tut5Desc = "Gebt das Handy herum. Wische die Karte, um zu sehen: ZIVILIST oder IMPOSTOR.",
            tut6Titulo = "🗣️ Die Debatte", tut6Desc = "Nutzt Hinweise, um den Impostor zu finden. Stimmt am Ende ab!",
            tut7Titulo = "🏆 Ergebnisse", tut7Desc = "Sieh, wer gewonnen hat. Drücke unten 'BEENDEN'.",
            tut8Titulo = "🌎 Community", tut8Desc = "Du kennst die Grundlagen! Lade unter 'Entdecken' andere Listen herunter.", tut8Btn = "BEENDEN",
            tutOmitir = "TUTORIAL ÜBERSPRINGEN"
        )
        IdiomaSoportado.CHINO -> TextosApp(
            menuInicio = "首页", menuBiblioteca = "图书馆", menuExplorar = "探索", menuPerfil = "个人资料",
            colabTitulo = "新合作！", colabDesc1 = "调查员 @", colabDesc2 = " 希望您帮助完成列表 '", colabDesc3 = "'。\n\n如果您接受，您可以编辑它。",
            colabAceptar = "接受", colabRechazar = "拒绝", colabExito = "您现在是合作者！",
            tut0Titulo = "🕵️ 欢迎！", tut0Desc = "Unmask 是终极卧底游戏。秘密在于列表：创建您的列表！", tut0Btn = "明白了！",
            tut1Titulo = "🆕 创建您的列表", tut1Desc = "按“立即创建”或底部的“+”。让我们做一个测试。",
            tut2Titulo = "📝 工作坊", tut2Desc = "给它命名并添加至少3个带有线索的单词来保存它。",
            tut3Titulo = "📚 您的图书馆", tut3Desc = "太棒了！这是您的创作。按“开始”进行设置。",
            tut4JugTitulo = "👥 添加朋友", tut4JugDesc = "写下名字并按“+”。至少需要3个。\n然后返回。",
            tut4ConfTitulo = "⚙️ 游戏设置", tut4ConfDesc = "在顶部添加3名玩家并调整规则。\n\n准备好后开始！",
            tut5Titulo = "🎭 您的角色", tut5Desc = "传递手机。滑动卡片查看您是平民还是卧底。",
            tut6Titulo = "🗣️ 辩论", tut6Desc = "说话并使用线索找出卧底。结束时投票！",
            tut7Titulo = "🏆 结果", tut7Desc = "看看谁赢了。按底部的“结束”。",
            tut8Titulo = "🌎 社区", tut8Desc = "您已了解基础知识！在“探索”中下载其他列表。登录并分享！", tut8Btn = "结束",
            tutOmitir = "跳过教程"
        )
        IdiomaSoportado.JAPONES -> TextosApp(
            menuInicio = "ホーム", menuBiblioteca = "ライブラリ", menuExplorar = "探索", menuPerfil = "プロフィール",
            colabTitulo = "新しいコラボ！", colabDesc1 = "調査員 @", colabDesc2 = " がリスト '", colabDesc3 = "' の手伝いを求めています。\n\n承認すると編集可能になります。",
            colabAceptar = "承認", colabRechazar = "拒否", colabExito = "コラボレーターになりました！",
            tut0Titulo = "🕵️ ようこそ！", tut0Desc = "Unmaskは究極のゲームです。秘密はリストにあります: 自分のリストを作成しましょう！", tut0Btn = "了解！",
            tut1Titulo = "🆕 リストを作成", tut1Desc = "「今すぐ作成」または下の「+」を押します。テストを作成しましょう。",
            tut2Titulo = "📝 ワークショップ", tut2Desc = "名前を付け、ヒント付きの単語を3つ以上追加して保存します。",
            tut3Titulo = "📚 ライブラリ", tut3Desc = "素晴らしい！これがあなたの作品です。「プレイ」を押して設定します。",
            tut4JugTitulo = "👥 友達を追加", tut4JugDesc = "名前を書いて「+」を押します。最低3人必要です。\nその後戻ります。",
            tut4ConfTitulo = "⚙️ ゲーム設定", tut4ConfDesc = "上に3人のプレイヤーを追加し、ルールを調整します。\n\n準備ができたら開始！",
            tut5Titulo = "🎭 あなたの役割", tut5Desc = "スマホを回してください。カードをスワイプして市民かインポスターか確認します。",
            tut6Titulo = "🗣️ ディベート", tut6Desc = "ヒントを使ってインポスターを見つけましょう。最後に投票！",
            tut7Titulo = "🏆 結果", tut7Desc = "勝者を確認します。下の「終了」を押してください。",
            tut8Titulo = "🌎 コミュニティ", tut8Desc = "基本は完了！「探索」で他のリストをダウンロードできます。ログインして共有！", tut8Btn = "終了",
            tutOmitir = "チュートリアルをスキップ"
        )
        // INGLÉS POR DEFECTO
        else -> TextosApp(
            menuInicio = "HOME", menuBiblioteca = "LIBRARY", menuExplorar = "EXPLORE", menuPerfil = "PROFILE",
            colabTitulo = "New Collaboration!", colabDesc1 = "Investigator @", colabDesc2 = " wants you to help fill the list '", colabDesc3 = "'.\n\nIf you accept, you can edit it.",
            colabAceptar = "ACCEPT", colabRechazar = "REJECT", colabExito = "You are now a collaborator!",
            tut0Titulo = "🕵️ Welcome!", tut0Desc = "Unmask is the ultimate imposter game app. The secret is in the lists: create your own!", tut0Btn = "GOT IT!",
            tut1Titulo = "🆕 Create your list", tut1Desc = "Press 'CREATE NOW' or the '+' below to start. Let's make a test one.",
            tut2Titulo = "📝 The Workshop", tut2Desc = "Give it a name and add at least 3 words with clues to save it.",
            tut3Titulo = "📚 Your Library", tut3Desc = "Great! Here are your creations. Press 'PLAY' to set it up.",
            tut4JugTitulo = "👥 Add friends", tut4JugDesc = "Write their names and press '+'. You need at least 3.\nThen go back.",
            tut4ConfTitulo = "⚙️ Game Settings", tut4ConfDesc = "Add 3 players at the top and adjust the rules.\n\nStart when ready!",
            tut5Titulo = "🎭 Your Role", tut5Desc = "Pass the phone around. Swipe the card to see if you are CIVIL or IMPOSTER.",
            tut6Titulo = "🗣️ The Debate", tut6Desc = "Talk and use the clues to discover the imposter. Vote at the end!",
            tut7Titulo = "🏆 Results", tut7Desc = "Check who won. Press 'FINISH' at the bottom to end.",
            tut8Titulo = "🌎 Community", tut8Desc = "You know the basics! In 'Explore' you can download others' lists. Log in and share!", tut8Btn = "FINISH",
            tutOmitir = "SKIP TUTORIAL"
        )
    }
}