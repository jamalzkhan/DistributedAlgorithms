#!/bin/bash
#
# ./sysmanager.sh [start|stop|clear]
#

PIDDIR="`pwd`/pids"

VERBOSE=true

# If true, stdout is redirected to
# log files, one per process.
LOG=true

USAGE="./sysmanager.sh [start|stop|clear] [class] [#instances]"

daemonize () {
	name=$1
	# The name of a process occurs twice; 
	# the first occurence is used for the
	# .pid file.
	shift 1
	(
	[[ -t 0 ]] && exec 0</dev/null
	if $LOG; then
		[[ -t 1 ]] && exec 1>`pwd`/${name}.out
	fi
	# Always redirect stderr to a file.
	[[ -t 2 ]] && exec 2>`pwd`/${name}.err
	
	# close non-standard file descriptors
	eval exec {3..255}\>\&-
	trap '' 1 2 # ignore HUP INT in child process
	exec "$@"
	) &
	pid=$!
	disown -h $pid
	$VERBOSE && echo "[DBG] ${name}'s pid is ${pid}"
	echo $pid > $PIDDIR/${name}.pid
	return 0
}

start () {
	$VERBOSE && echo "[DBG] start ${N} instances of class ${P}"
	i=1
	while [ $i -le $N ]; do
		name="P${i}"
		# You can append arguments args[3], args[4],
		# and so on after ${N}.
		daemonize ${name} java ${P} ${name} ${i} ${N} $@
		let i++
	done
}

clear () {
	$VERBOSE && echo "[DBG] clear"
	[ -d "$PIDDIR" ] && rm -f $PIDDIR/*.pid
	rm -rf $PIDDIR
	# Delete empty *.err files
	ls *.err &>/dev/null
	if [ $? -eq 0 ]; then
		files=`ls *.err`
		for f in $files; do
			[ ! -s $f ] && rm -f $f
		done
	fi
}

stop () {
	$VERBOSE && echo "[DBG] stop"
	[ ! -d $PIDDIR ] && return 0
	ls $PIDDIR/*.pid &>/dev/null
	if [ $? -eq 0 ]; then
		files=`ls $PIDDIR/*.pid`
		for f in $files; do
			pid=`cat $f`
			$VERBOSE && echo "[DBG] pid is $pid"
			kill -9 $pid &>/dev/null
			rm -f $f
		done
	fi
	clear # delete $PIDDIR.
}

#
# main ($1) ($2) ($3) (...)
#
if [ $# -lt 1 ]; then
	echo $USAGE && exit 1
else
	if [ $1 == "start" ]; then
		
		if [ $# -lt 3 ]; then # Check number of arguments.
			echo $USAGE
			exit 1
		fi
		
		if [ ! -f "$2.class" ]; then # Check program name.
			echo "error: $2.class not found"
			exit 1
		fi
		# Check number of processes.
		[ $3 -eq $3 ] >/dev/null 2>&1
		if [ $? -eq 1 ]; then
			echo "error: invalid argument ($3)"
			exit 1
		fi
		
		if [ $3 -le 0 ]; then
			echo "error: invalid argument ($3)"
			exit 1
		fi
		
	elif [ $# -ne 1 ]; then
			echo $USAGE
			exit 1
	fi
fi

C=$1 # start, stop, or clear
P=$2 # class name
N=$3 # #instances
shift 3
# Arguments 4, 5, 6, ... are passed to the process when it starts.

case ${C} in
	"start")
	
	[ -d "$PIDDIR" ] || mkdir -p $PIDDIR
	
	# To begin with, start NameServer
	daemonize "P0" java Registrar $N
	
	start $@ ;;
	"stop" )
	stop ;;
	"clear")
	clear;;
	*)
	echo $USAGE
	exit 1
esac

exit 0

