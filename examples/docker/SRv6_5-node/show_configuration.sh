#!/usr/bin/env bash
echo -e "\n\033[1mvppA Configuration:"
echo -e "----------------------------------------------------------------------------------------\033[m"
sudo docker exec vppA vppctl sh sr localsids
sudo docker exec vppA vppctl sh sr policies
sudo docker exec vppA vppctl sh sr steering
echo -e "\n\033[1mvppB Configuration:"
echo -e "----------------------------------------------------------------------------------------\033[m"
sudo docker exec vppB vppctl sh sr localsids
echo -e "\n\033[1mvppC Configuration:"
echo -e "----------------------------------------------------------------------------------------\033[m"
sudo docker exec vppC vppctl sh sr localsids
echo -e "\n\033[1mvppD Configuration:"
echo -e "----------------------------------------------------------------------------------------\033[m"
sudo docker exec vppD vppctl sh sr localsids
echo -e "\n\033[1mvppE Configuration:"
echo -e "----------------------------------------------------------------------------------------\033[m"
sudo docker exec vppE vppctl sh sr localsids
sudo docker exec vppE vppctl sh sr policies
sudo docker exec vppE vppctl sh sr steering
