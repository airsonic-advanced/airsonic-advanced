<!DOCTYPE html>
<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8"%>

<html><head>
    <%@ include file="head.jsp" %>
    <%@ include file="jquery.jsp" %>
    <script type="text/javascript" src="<c:url value='/script/chart-3.5.0.min.js'/>"></script>
    <meta http-equiv="CACHE-CONTROL" content="NO-CACHE">
    <style>
      #sessionsTable td, .cacheusage, .cachehits, .cachemiss, .cacheputs, .cacheremovals {
        text-align: center;
      }
    </style>
<script>
  const labels = {
    null: '<fmt:message key="common.unknown"/>',
    'download': '<fmt:message key="status.download"/>',
    'upload': '<fmt:message key="status.upload"/>',
    'stream': '<fmt:message key="status.stream"/>',
    'never': '<fmt:message key="status.never"/>',
    'incalculable': '<fmt:message key="status.incalculable"/>',
    'RUN_ONCE': '<fmt:message key="status.scheduledrunonce"/>',
    'FIXED_DELAY': '<fmt:message key="status.scheduledfixeddelay"/>',
    'FIXED_RATE': '<fmt:message key="status.scheduledfixedrate"/>',
  };

  const transferTypeColors = {
    // yellow
    stream: {
      bg: 'rgba(255, 205, 86, 0.5)',
      bo: 'rgb(255, 205, 86)'
    },
    // red
    download: {
      bg: 'rgba(255, 99, 132, 0.5)',
      bo: 'rgb(255, 99, 132)'
    },
    // blue
    upload: {
      bg: 'rgba(54, 162, 235, 0.5)',
      bo: 'rgb(54, 162, 235)'
    }
  };

  const transfersConfigTemplate = {
    type: 'scatter',
    options: {
      responsive: true,
      plugins: {
        legend: {
          display: false
        }
      },
      scales: {
        x: {
          ticks: {
            callback: function(value, index, values) {
              return new Date(value).toLocaleString();
            }
          }
        }
      }
    }
  };

  var transfersCharts = [];

  var transfersUrl = "<c:url value='/statistics/transfers'/>";

  function updateTransferData() {
   $.get(transfersUrl, data => {
    $('#transfersTable > tbody').empty();

    transfersCharts.forEach(tc => tc.destroy());
    transfersCharts = [];
    var appendedRows = '';
    data.forEach((row, i) => {
        appendedRows += '<tr>';
        appendedRows +=   '<td>' + labels[row.transferType] + '</td>';
        appendedRows +=   '<td>' + row.playerDescription + '<br>' + row.playerType + '</td>';
        appendedRows +=   '<td>' + row.username + '</td>';
        appendedRows +=   '<td>' + row.path + '</td>';
        appendedRows +=   '<td>' + row.bytesTransferred + '</td>';
        appendedRows +=   '<td><canvas id="transfersChart' + i + '"></canvas></td>';
        appendedRows += '</tr>';
    });

    $('#transfersTable > tbody').append(appendedRows);

    data.forEach((row, i) => {
        var series = [];
        if (row.history.length > 0) {

            var previous = row.history[0];

            for (var j = 1; j < row.history.length; j++) {
                var sample = row.history[j];

                var elapsedTimeMillis = sample.timestamp - previous.timestamp;
                var bytesStreamed = Math.max(0, sample.bytesTransferred - previous.bytesTransferred);

                var kbps = (8.0 * bytesStreamed / 1024.0) / (elapsedTimeMillis / 1000.0);
                series.push({x: sample.timestamp, y: kbps});

                previous = sample;
            }
        }

        var config = $.extend({
          data: {
            datasets: [{
              data: series,
              showLine: true,
              backgroundColor: transferTypeColors[row.transferType]['bg'],
              borderColor: transferTypeColors[row.transferType]['bo'],
              borderWidth: 2
            }]
          }
        }, transfersConfigTemplate);

        transfersCharts[i] = new Chart($('#transfersChart' + i), config);
    });
   });
  }

  const userChartConfig = {
    type: 'bar',
    data: {
      datasets: [{
            label: '<fmt:message key="home.chart.stream"/>',
            data: [],
            backgroundColor: transferTypeColors['stream']['bg'],
            borderColor: transferTypeColors['stream']['bo'],
            borderWidth: 1,
            parsing: {
                xAxisKey: 'streamed',
                yAxisKey: 'user'
            }
        },{
            label: '<fmt:message key="home.chart.download"/>',
            data: [],
            backgroundColor: transferTypeColors['download']['bg'],
            borderColor: transferTypeColors['download']['bo'],
            borderWidth: 1,
            parsing: {
                xAxisKey: 'downloaded',
                yAxisKey: 'user'
            }
        },{
            label: '<fmt:message key="home.chart.upload"/>',
            data: [],
            backgroundColor: transferTypeColors['upload']['bg'],
            borderColor: transferTypeColors['upload']['bo'],
            borderWidth: 1,
            parsing: {
                xAxisKey: 'uploaded',
                yAxisKey: 'user'
            }
        }]
    },
    options: {
      responsive: true,
      indexAxis: 'y',
      scales: {
        x: {
          stacked: true
        },
        y: {
          stacked: true
        }
      }
    }
  };

  var userChartUrl = "<c:url value='/statistics/users'/>";
  var userChart = null;

  function updateUserChartData() {
   $.get(userChartUrl, data => {
    data.forEach(x => {
      x.streamed = x.streamed / (1024 * 1024);
      x.downloaded = x.downloaded / (1024 * 1024);
      x.uploaded = x.uploaded/ (1024 * 1024);
    });
    userChart.data.datasets[0].data = data;
    userChart.data.datasets[1].data = data;
    userChart.data.datasets[2].data = data;
    userChart.update();
   });
  }

  var airsonicCaches = [];
  var cacheNamesUrl = "<c:url value='/actuator/metrics/cache.gets'/>";

  var cacheMissUrl = "<c:url value='/actuator/metrics/cache.gets?tag=result:miss&'/>";
  var cacheHitsUrl = "<c:url value='/actuator/metrics/cache.gets?tag=result:hit&'/>";
  var cachePutsUrl = "<c:url value='/actuator/metrics/cache.puts?'/>";
  var cacheRemovalsUrl = "<c:url value='/actuator/metrics/cache.removals?'/>";
  var cacheEvictionssUrl = "<c:url value='/actuator/caches/'/>";

  var updateCacheUsage = c => {
    var hit = +($('#cachesTable > tbody .' + c + ' .cachehits').text());
    var miss = +($('#cachesTable > tbody .' + c + ' .cachemiss').text());
    var usage = "N/A";
    if (!isNaN(hit) && !isNaN(miss) && (hit+miss != 0)) {
      usage = hit / (hit + miss) * 100.0;
    }
    $('#cachesTable > tbody .' + c + ' .cacheusage').text(usage);
  };
  var updateCacheMissData = c => $.get(cacheMissUrl+"tag=cache:"+c, data => {
    $('#cachesTable > tbody .' + c + ' .cachemiss').text(data.measurements[0].value);
    updateCacheUsage(c);
  });
  var updateCacheHitsData = c => $.get(cacheHitsUrl+"tag=cache:"+c, data => {
    $('#cachesTable > tbody .' + c + ' .cachehits').text(data.measurements[0].value);
    updateCacheUsage(c);
  });
  var updateCachePutsData = c => $.get(cachePutsUrl+"tag=cache:"+c, data => {
    $('#cachesTable > tbody .' + c + ' .cacheputs').text(data.measurements[0].value);
  });
  var updateCacheRemovalsData = c => $.get(cacheRemovalsUrl+"tag=cache:"+c, data => {
    $('#cachesTable > tbody .' + c + ' .cacheremovals').text(data.measurements[0].value);
  });
  var evictCache = c => $.ajax(cacheEvictionssUrl+c, {type: 'DELETE'}).always((data, status) => {
    console.log("eviction request for " + c + " completed with status: " + status);
    updateCacheData(c);
  });
  var updateCacheData = c => {
    updateCacheMissData(c);
    updateCacheHitsData(c);
    updateCachePutsData(c);
    updateCacheRemovalsData(c);
  };
  function updateCachesData() {
    airsonicCaches.forEach(c => updateCacheData(c));
  };

  var parseScheduledDate = date => {
    if (date == null) {
      return labels['never'];
    }
    if (date == "-1000000000-01-01T00:00Z") {
      return labels['incalculable'];
    }
    try {
      return new Date(date);
    } catch (e) {
      return date;
    }
  };

  var scheduledTasksUrl = "<c:url value='/actuator/customscheduledtasks'/>";
  function updateScheduledTasksData() {
    $.get(scheduledTasksUrl, data => {
      $('#scheduledTasksTable > tbody').empty();
      var appendedRows = '';
      Object.keys(data).forEach(k => {
        var taskData = data[k];
        appendedRows += '<tr>';
        appendedRows +=   '<td>' + taskData['name'] + '</td>';
        appendedRows +=   '<td>' + labels[taskData['runMetadata']['type']] + '</td>';
        appendedRows +=   '<td>' + parseScheduledDate(taskData['created']) + '</td>';
        appendedRows +=   '<td>' + parseScheduledDate(taskData['runMetadata']['firstRun']) + '</td>';
        appendedRows +=   '<td>' + parseScheduledDate(taskData['runMetadata']['lastRun']) + '</td>';
        appendedRows +=   '<td>' + parseScheduledDate(taskData['runMetadata']['nextRun']) + '</td>';
        appendedRows +=   '<td>' + taskData['scheduledBy'] + '</td>';
        appendedRows += '</tr>';
      });

      $('#scheduledTasksTable > tbody').append(appendedRows);
    });
  }

  var pathWatcherUrl = "<c:url value='/actuator/pathwatcher'/>";
  function updatePathWatcherData() {
    $.get(pathWatcherUrl, data => {
      $('#pathWatcherTable > tbody').empty();
      var appendedRows = '';
      Object.keys(data).forEach(k => {
        appendedRows += '<tr>';
        appendedRows +=   '<td>' + k + '</td>';
        appendedRows +=   '<td>' + data[k] + '</td>';
        appendedRows += '</tr>';
      });

      $('#pathWatcherTable > tbody').append(appendedRows);
    });
  }

  var sessionsCurrentUrl = "<c:url value='/actuator/metrics/tomcat.sessions.active.current'/>";
  var sessionsCreatedUrl = "<c:url value='/actuator/metrics/tomcat.sessions.created'/>";
  var sessionsExpiredUrl = "<c:url value='/actuator/metrics/tomcat.sessions.expired'/>";
  var sessionsRejectedUrl = "<c:url value='/actuator/metrics/tomcat.sessions.rejected'/>";

  var updateSessionsCurrentData = () => $.get(sessionsCurrentUrl, data => {
    $('#sessionsTable > tbody .sessionscurrent').text(data.measurements[0].value);
  });
  var updateSessionsCreatedData = () => $.get(sessionsCreatedUrl, data => {
    $('#sessionsTable > tbody .sessionscreated').text(data.measurements[0].value);
  });
  var updateSessionsExpiredData = () => $.get(sessionsExpiredUrl, data => {
    $('#sessionsTable > tbody .sessionsexpired').text(data.measurements[0].value);
  });
  var updateSessionsRejectedData = () => $.get(sessionsRejectedUrl, data => {
    $('#sessionsTable > tbody .sessionsrejected').text(data.measurements[0].value);
  });
  function updateSessionsData() {
    updateSessionsCurrentData();
    updateSessionsCreatedData();
    updateSessionsExpiredData();
    updateSessionsRejectedData();
  };

  var healthUrl = "<c:url value='/actuator/health'/>";
  function updateHealthData() {
    $.get(healthUrl).always(data => {
      $('#healthTable > tbody').empty();
      var appendedRows = '';
      var dc = (typeof data.responseJSON != 'undefined' && typeof data.responseJSON.components != 'undefined') ? data.responseJSON.components : data.components;
      if (typeof dc != 'undefined') {
        Object.keys(dc).forEach(k => {
          appendedRows += '<tr>';
          appendedRows +=   '<td>' + k + '</td>';
          appendedRows +=   '<td>' + dc[k].status + '</td>';
          appendedRows +=   '<td>' + JSON.stringify(dc[k].details) + '</td>';
          appendedRows += '</tr>';
        });
      }

      $('#healthTable > tbody').append(appendedRows);
    });
  }

  function init() {
    userChart = new Chart(
      $('#userChart'),
      userChartConfig
    );

    $.get(cacheNamesUrl, data => {
      airsonicCaches = data.availableTags.filter(i => i.tag == 'cache')[0].values.sort();
      var appendedRows = '';
      airsonicCaches.forEach((row, i) => {
        appendedRows += '<tr class="' + row + '">';
        appendedRows +=   '<td class="cachename">' + row + '</td>';
        appendedRows +=   '<td class="cacheusage"></td>';
        appendedRows +=   '<td class="cachehits"></td>';
        appendedRows +=   '<td class="cachemiss"></td>';
        appendedRows +=   '<td class="cacheputs"></td>';
        appendedRows +=   '<td class="cacheremovals"></td>';
        appendedRows +=   '<td class="cacheevictions"><button onclick="evictCache(\''+row+'\')"><fmt:message key="status.cacheevict"/></button></td>';
        appendedRows += '</tr>';
      });

      $('#cachesTable > tbody').append(appendedRows);
      updateCachesData();
    });

    updateTransferData();
    updateUserChartData();
    updateScheduledTasksData();
    updatePathWatcherData();
    updateSessionsData();
    updateHealthData();

    setInterval(() => { updateTransferData(); updateUserChartData(); updateCachesData(); updateScheduledTasksData(); updatePathWatcherData(); updateSessionsData(); updateHealthData();}, 40000);
  }
</script>

</head>
<body class="mainframe bgcolor1" onload="init()">

<h1>
    <img src="<spring:theme code='statusImage'/>" alt="">
    <span style="vertical-align: middle"><fmt:message key="status.title"/></span>
</h1>
<h2>
  <fmt:message key="status.currenttransfers"/>
</h2>
<table id="transfersTable" width="100%" class="ruleTable indent">
    <thead>
      <tr>
        <th class="ruleTableHeader"><fmt:message key="status.type"/></th>
        <th class="ruleTableHeader"><fmt:message key="status.player"/></th>
        <th class="ruleTableHeader"><fmt:message key="status.user"/></th>
        <th class="ruleTableHeader"><fmt:message key="status.current"/></th>
        <th class="ruleTableHeader"><fmt:message key="status.transmitted"/></th>
        <th class="ruleTableHeader"><fmt:message key="status.bitrate"/></th>
      </tr>
    </thead>
    <tbody>
    </tbody>
</table>
<div style="padding-top:3em"></div>

<h2>
  <fmt:message key="status.usertransfers"/>
</h2>
<canvas id="userChart"></canvas>
<div style="padding-top:3em"></div>

<h2>
  <fmt:message key="status.caches"/>
</h2>
<table id="cachesTable" width="100%" class="ruleTable indent">
    <thead>
      <tr>
        <th class="ruleTableHeader"><fmt:message key="status.cachename"/></th>
        <th class="ruleTableHeader"><fmt:message key="status.cacheusage"/></th>
        <th class="ruleTableHeader"><fmt:message key="status.cachehits"/></th>
        <th class="ruleTableHeader"><fmt:message key="status.cachemiss"/></th>
        <th class="ruleTableHeader"><fmt:message key="status.cacheputs"/></th>
        <th class="ruleTableHeader"><fmt:message key="status.cacheremovals"/></th>
        <th class="ruleTableHeader"><fmt:message key="status.cacheevict"/></th>
      </tr>
    </thead>
    <tbody>
    </tbody>
</table>
<div style="padding-top:3em"></div>

<h2>
  <fmt:message key="status.scheduledtasks"/>
</h2>
<table id="scheduledTasksTable" width="100%" class="ruleTable indent">
    <thead>
      <tr>
        <th class="ruleTableHeader"><fmt:message key="status.name"/></th>
        <th class="ruleTableHeader"><fmt:message key="status.scheduledtype"/></th>
        <th class="ruleTableHeader"><fmt:message key="status.scheduledcreated"/></th>
        <th class="ruleTableHeader"><fmt:message key="status.scheduledfirstrun"/></th>
        <th class="ruleTableHeader"><fmt:message key="status.scheduledlastrun"/></th>
        <th class="ruleTableHeader"><fmt:message key="status.schedulednextrun"/></th>
        <th class="ruleTableHeader"><fmt:message key="status.scheduledby"/></th>
      </tr>
    </thead>
    <tbody>
    </tbody>
</table>
<div style="padding-top:3em"></div>

<h2>
  <fmt:message key="status.pathwatcher"/>
</h2>
<table id="pathWatcherTable" width="100%" class="ruleTable indent">
    <thead>
      <tr>
        <th class="ruleTableHeader"><fmt:message key="status.name"/></th>
        <th class="ruleTableHeader"><fmt:message key="status.path"/></th>
      </tr>
    </thead>
    <tbody>
    </tbody>
</table>
<div style="padding-top:3em"></div>

<h2>
  <fmt:message key="status.sessions"/>
</h2>
<table id="sessionsTable" width="100%" class="ruleTable indent">
    <thead>
      <tr>
        <th class="ruleTableHeader"><fmt:message key="status.sessionscurrent"/></th>
        <th class="ruleTableHeader"><fmt:message key="status.sessionscreated"/></th>
        <th class="ruleTableHeader"><fmt:message key="status.sessionsexpired"/></th>
        <th class="ruleTableHeader"><fmt:message key="status.sessionsrejected"/></th>
      </tr>
    </thead>
    <tbody>
      <tr>
        <td class="sessionscurrent">N/A</td>
        <td class="sessionscreated">N/A</td>
        <td class="sessionsexpired">N/A</td>
        <td class="sessionsrejected">N/A</td>
      </tr>
    </tbody>
</table>

<div style="padding-top:3em"></div>

<h2>
  <fmt:message key="status.health"/>
</h2>
<table id="healthTable" width="100%" class="ruleTable indent">
    <thead>
      <tr>
        <th class="ruleTableHeader"><fmt:message key="status.component"/></th>
        <th class="ruleTableHeader"><fmt:message key="status.status"/></th>
        <th class="ruleTableHeader"><fmt:message key="status.details"/></th>
      </tr>
    </thead>
    <tbody>
    </tbody>
</table>

<div class="forward"><a href="status.view?"><fmt:message key="common.refresh"/> (<fmt:message key="status.autorefresh"><fmt:param>${40000/1000}</fmt:param></fmt:message>)</a></div>

</body></html>
