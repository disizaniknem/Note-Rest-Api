package com.disizaniknem.data

import com.disizaniknem.data.collections.Note
import com.disizaniknem.data.collections.User
import io.ktor.html.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.litote.kmongo.contains
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.eq
import org.litote.kmongo.reactivestreams.KMongo
import org.litote.kmongo.setValue

private val client = KMongo.createClient().coroutine
private val database = client.getDatabase("NotesDatabase")
private val users = database.getCollection<User>()
private val notes = database.getCollection<Note>()

suspend fun getNotesForUser(email: String) : List<Note?> {

    // Test 1
    /*val listOfNotes = mutableListOf<Note>()

    database.listCollections<Note>().toList().forEach {  note ->
        for (owner in note.owners) {
            if (owner == email) {
                listOfNotes += note
            }
        }
    }

    return listOfNotes*/

    // Test 2
    return notes.find(Note::owners contains email).toList()

    // Test 3
    /*val id = "whatever"
    return listOf(notes.findOne(Note::id eq id))*/
}

suspend fun registerUser(user: User): Boolean {
    return users.insertOne(user).wasAcknowledged()
}

suspend fun checkIfUserExists(email: String) : Boolean {
    return users.findOne(User::email eq email) != null
}

suspend fun checkPasswordForEmail(email: String, passwordToCheck: String) : Boolean {
    val actualPassword = users.findOne(User::email eq email)?.password ?: return false
    return actualPassword == passwordToCheck
}

suspend fun saveNote(note: Note): Boolean {
    val noteExists = notes.findOneById(note.id) != null
    return if (noteExists) {
        notes.updateOneById(note.id, note).wasAcknowledged()
    } else {
        notes.insertOne(note).wasAcknowledged()
    }
}

suspend fun deleteNoteForUser(email: String, noteId: String) : Boolean {
    val note = notes.findOne(Note::id eq noteId, Note::owners contains email)
    note?.let { note ->
        if (note.owners.size > 1) {
            val newOwners = note.owners - email
            val updateResult = notes.updateOne(Note::id eq note.id, setValue(Note::owners, newOwners))
            return updateResult.wasAcknowledged()
        }

        return notes.deleteOneById(note.id).wasAcknowledged()
    } ?: return false
}

suspend fun addOwnerToNote(noteId: String, owner: String) : Boolean {
    val owners = notes.findOneById(noteId)?.owners ?: return false
    return notes.updateOneById(noteId, setValue(Note::owners, owners + owner)).wasAcknowledged()
}

suspend fun isOwnerOfNote(noteId: String, owner: String): Boolean {
    val note = notes.findOneById(noteId) ?: return false
    return owner in note.owners
}