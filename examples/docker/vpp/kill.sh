#!/bin/bash

ps -ef | grep vpp | awk '{print $2}'| xargs kill
