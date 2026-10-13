% Lab 3 step 4 - MATLAB Visualization: all 4 fields of channel 1 on one plot.
% Paste into ThingSpeak: channel 1 -> MATLAB Visualization -> Custom.
% Name: Remote Weather Station Data.
% ASCII-only: ThingSpeak code checker may fail on Cyrillic comments.

readChId = 0;                   % channel 1 ID
readKey = 'READ_KEY_CH1';       % Read API Key of channel 1

[RemoteWeatherStationData,timeStamps] = thingSpeakRead(readChId, ...
    'fields',[1,2,3,4], 'NumPoints',500,'ReadKey',readKey);

plot(timeStamps,RemoteWeatherStationData,'s');
xlabel('TimeStamps');
ylabel('Measured Values');
title('Remote Weather Station Data');
legend({'Temperature','Humidity','Pressure', 'Illumination'});
grid on;
