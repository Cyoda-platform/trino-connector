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


select * from cyoda.reporting."cyoda-leimessage-all lei messages"
where reportId = '000186c8-0000-1000-8080-808080808080-LEIMessage-a6aabec0-d476-11ec-9492-926df8204c70'
  and groupingversion = '00000000-0000-1000-0000-000000000000';

-- select * from cyoda.reporting."play-interfacemessage-patrick-trade-extract-fix"
-- where reportId = '000186be-0000-1000-8080-808080808080-InterfaceMessage-8a461ee0-d6a2-11ec-b0b1-901b0ebd9b67'
-- and groupingversion = '00000000-0000-1000-0000-000000000000';
