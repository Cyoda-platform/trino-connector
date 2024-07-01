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

use cyoda.reporting;
select * from reports;
select * from report_details;
select * from report_histories;
select * from report_groups;
select * from report_stats;

select r.reportname, s.id,s.createtime,s.groupscount,s.totalrowscount from report_stats s, reports r
where s.configname = r.id
  and s.totalrowscount > 0;




