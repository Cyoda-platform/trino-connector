/*
 * Copyright (C) 2022 Cyoda Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */


-- select * from cyoda.reporting."cyoda-leimessage-all lei messages"
-- where reportId = '000186c8-0000-1000-8080-808080808080-LEIMessage-a6aabec0-d476-11ec-9492-926df8204c70'
--   and groupingversion = '00000000-0000-1000-0000-000000000000';

use cyoda.reporting;
select * from reports;
select * from report_details;
select * from report_histories;
select * from report_groups;
select * from report_stats;

select reportid from (
    select reportid, max(createtime) from report_histories where configname = 'PLAY-InterfaceMessage-FIX Extract Report'
    group by reportid
);



select * from cyoda.reporting."play-interfacemessage-fix extract report"
where reportId in (
    select reportid from (
        select reportid, max(createtime) from report_histories where configname = 'PLAY-InterfaceMessage-FIX Extract Report'
        and groupingversion = '00000000-0000-1000-0000-000000000000'
        group by reportid
        )
    )
and groupingversion = '00000000-0000-1000-0000-000000000000';

-- Demo
-- select * from cyoda.reporting."play-interfacemessage-fix extract report"
--     where reportid = '000186be-0000-1000-8080-808080808080-InterfaceMessage-e19017f0-dd28-11ec-9424-901b0e8e460e'
--     and groupingversion = '00000000-0000-1000-0000-000000000000';

-- Local
select * from cyoda.reporting."play-interfacemessage-fix extract report"
where reportid = '000186c8-0000-1000-8080-808080808080-InterfaceMessage-7298fc00-e173-11ec-90e5-967e648b60e9'
  and groupingversion = '00000000-0000-1000-0000-000000000000'
and rownum between 100 and 1000;
--and rownum between 5100 and 5200;