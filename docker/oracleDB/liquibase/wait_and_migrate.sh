#!/usr/bin/env bash
#Wait for db to go up (for 10 minutes) and run liquibase migrate

nohup /home/oracle/wait_for_it.sh localhost:1521 -t 600 -- /home/oracle/run_image.sh &