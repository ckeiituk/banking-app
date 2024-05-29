package ru.banking.client

import tornadofx.App
import tornadofx.launch

class ClientApp : App(LoginView::class)

fun main(args: Array<String>) {
    launch<ClientApp>(args)
}
