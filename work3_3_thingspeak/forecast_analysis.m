% Lab 3 step 3 - MATLAB Analysis: weather forecast by pressure.
% Paste into ThingSpeak: Apps -> MATLAB Analysis -> Custom.
% ASCII-only: ThingSpeak code checker may fail on Cyrillic comments.

readChId = 0;            % channel 1 ID (weather station data)
writeChId = 0;           % channel 2 ID (forecast, 1 field)
writeKey = 'WRITE_KEY_CH2'; % Write API Key of channel 2
readKey = 'READ_KEY_CH1';    % Read API Key of channel 1

[pressure,time] = thingSpeakRead(readChId,'Fields',3,'NumPoints',1, ...
    'ReadKey',readKey);

LEVEL_1 = 767.2;  % mm Hg
LEVEL_2 = 756.92; % mm Hg

if isempty(pressure)
    text = 'no data yet';
elseif (pressure >= LEVEL_1)
    text = 'The weather will be stable';
elseif and(pressure >= LEVEL_2,pressure <= LEVEL_1)
    text = 'The weather will be cloudy';
else
    text = 'The weather will be rainy';
end

% Values as cell of char; TimeStamps omitted - entry gets current time.
% (Passing a datetime/string here errors on current ThingSpeak version.)
thingSpeakWrite(writeChId,'Fields',1,'Values',{text},'Writekey',writeKey);
disp(text);
