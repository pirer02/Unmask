package org.example.project.Datos.TextosTraducidos

import org.example.project.Datos.IdiomaSoportado

data class TextosJugadores(
    val tituloJugadores: String,
    val placeholderNombre: String,
    val anadeMinimo: String,
    val errorVacio: String,
    val errorExiste: String,
    val dialogoBorrarTitulo: String,
    val dialogoBorrarDesc: String,
    val btnVaciar: String,
    val btnCancelar: String
)

fun obtenerTextosJugadores(idioma: IdiomaSoportado): TextosJugadores {
    return when (idioma) {
        IdiomaSoportado.ESPANOL -> TextosJugadores(
            tituloJugadores = "Jugadores",
            placeholderNombre = "Nombre del jugador...",
            anadeMinimo = "Añade al menos 3 jugadores",
            errorVacio = "El nombre no puede estar vacío",
            errorExiste = "¡Este nombre ya existe!",
            dialogoBorrarTitulo = "¿Borrar todos?",
            dialogoBorrarDesc = "Se vaciará la lista de jugadores.",
            btnVaciar = "VACIAR",
            btnCancelar = "CANCELAR"
        )
        IdiomaSoportado.FRANCES -> TextosJugadores(
            tituloJugadores = "Joueurs",
            placeholderNombre = "Nom du joueur...",
            anadeMinimo = "Ajoutez au moins 3 joueurs",
            errorVacio = "Le nom ne peut pas être vide",
            errorExiste = "Ce nom existe déjà !",
            dialogoBorrarTitulo = "Tout supprimer ?",
            dialogoBorrarDesc = "La liste des joueurs sera vidée.",
            btnVaciar = "VIDER",
            btnCancelar = "ANNULER"
        )
        IdiomaSoportado.ITALIANO -> TextosJugadores(
            tituloJugadores = "Giocatori",
            placeholderNombre = "Nome del giocatore...",
            anadeMinimo = "Aggiungi almeno 3 giocatori",
            errorVacio = "Il nome non può essere vuoto",
            errorExiste = "Questo nome esiste già!",
            dialogoBorrarTitulo = "Eliminare tutti?",
            dialogoBorrarDesc = "La lista dei giocatori verrà svuotata.",
            btnVaciar = "SVUOTA",
            btnCancelar = "ANNULLA"
        )
        IdiomaSoportado.ALEMAN -> TextosJugadores(
            tituloJugadores = "Spieler",
            placeholderNombre = "Spielername...",
            anadeMinimo = "Füge mindestens 3 Spieler hinzu",
            errorVacio = "Name darf nicht leer sein",
            errorExiste = "Dieser Name existiert bereits!",
            dialogoBorrarTitulo = "Alle löschen?",
            dialogoBorrarDesc = "Die Spielerliste wird geleert.",
            btnVaciar = "LEEREN",
            btnCancelar = "ABBRECHEN"
        )
        IdiomaSoportado.CHINO -> TextosJugadores(
            tituloJugadores = "玩家",
            placeholderNombre = "玩家名称...",
            anadeMinimo = "至少添加3名玩家",
            errorVacio = "名称不能为空",
            errorExiste = "此名称已存在！",
            dialogoBorrarTitulo = "全部删除？",
            dialogoBorrarDesc = "玩家列表将被清空。",
            btnVaciar = "清空",
            btnCancelar = "取消"
        )
        IdiomaSoportado.JAPONES -> TextosJugadores(
            tituloJugadores = "プレイヤー",
            placeholderNombre = "プレイヤー名...",
            anadeMinimo = "少なくとも3人のプレイヤーを追加してください",
            errorVacio = "名前を空にすることはできません",
            errorExiste = "この名前は既に存在します！",
            dialogoBorrarTitulo = "すべて削除しますか？",
            dialogoBorrarDesc = "プレイヤーリストが空になります。",
            btnVaciar = "空にする",
            btnCancelar = "キャンセル"
        )
        // INGLÉS POR DEFECTO
        else -> TextosJugadores(
            tituloJugadores = "Players",
            placeholderNombre = "Player name...",
            anadeMinimo = "Add at least 3 players",
            errorVacio = "Name cannot be empty",
            errorExiste = "This name already exists!",
            dialogoBorrarTitulo = "Delete all?",
            dialogoBorrarDesc = "The player list will be emptied.",
            btnVaciar = "EMPTY",
            btnCancelar = "CANCEL"
        )
    }
}