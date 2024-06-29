package com.example.nearchat.domain.chat

import java.io.IOException

class TransferFailedException : IOException("Reading incomming message is failed")